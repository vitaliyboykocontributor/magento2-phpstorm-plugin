/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.magento.idea.magento2plugin.actions.generation.data.ModuleComposerJsonData;
import com.magento.idea.magento2plugin.actions.generation.data.ModuleReadmeMdData;
import com.magento.idea.magento2plugin.actions.generation.data.ModuleRegistrationPhpData;
import com.magento.idea.magento2plugin.actions.generation.data.ModuleXmlData;
import com.magento.idea.magento2plugin.actions.generation.generator.ModuleComposerJsonGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.ModuleReadmeMdGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.ModuleRegistrationPhpGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.ModuleXmlGenerator;
import com.magento.idea.magento2plugin.mcp.model.ModuleCreationRequest;
import com.magento.idea.magento2plugin.mcp.model.ModuleCreationResponse;
import com.magento.idea.magento2plugin.mcp.util.JsonUtil;
import com.magento.idea.magento2plugin.mcp.util.McpPathUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for module creation requests.
 */
public class ModuleCreationHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getInstance(ModuleCreationHandler.class);
    private final Project project;

    /**
     * Constructor.
     *
     * @param project Current project
     */
    public ModuleCreationHandler(@NotNull final Project project) {
        this.project = project;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed. Use POST.");
                return;
            }

            // Parse request body
            final InputStream requestBody = exchange.getRequestBody();
            final String requestString = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
            final ModuleCreationRequest request = JsonUtil.fromJson(requestString, ModuleCreationRequest.class);

            if (request == null) {
                sendResponse(exchange, 400, "Invalid request format");
                return;
            }

            // Validate request
            final String validationError = validateRequest(request);
            if (validationError != null) {
                sendResponse(exchange, 400, validationError);
                return;
            }

            // Generate module
            final ModuleCreationResponse response = generateModule(request);
            
            // Send response
            final String responseJson = JsonUtil.toJson(response);
            sendResponse(exchange, 200, responseJson);
        } catch (Exception e) {
            LOGGER.error("Error handling module creation request: " + e.getMessage(), e);
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Validate the module creation request.
     *
     * @param request Module creation request
     * @return Validation error message or null if valid
     */
    private String validateRequest(final ModuleCreationRequest request) {
        if (request.getPackageName() == null || request.getPackageName().isEmpty()) {
            return "Package name is required";
        }
        if (request.getModuleName() == null || request.getModuleName().isEmpty()) {
            return "Module name is required";
        }
        if (request.getModuleDescription() == null || request.getModuleDescription().isEmpty()) {
            return "Module description is required";
        }
        if (request.getModuleVersion() == null || request.getModuleVersion().isEmpty()) {
            return "Module version is required";
        }
        return null;
    }

    /**
     * Generate the module files.
     *
     * @param request Module creation request
     * @return Module creation response
     */
    private ModuleCreationResponse generateModule(final ModuleCreationRequest request) {
        final ModuleCreationResponse response = new ModuleCreationResponse();
        response.setSuccess(false);

        try {
            // Find the base directory for module creation
            final PsiDirectory baseDir = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiDirectory>) () -> McpPathUtil.findBaseDirectoryByVendor(project, request.getPackageName())
            );

            if (baseDir == null) {
                String magentoPath = com.magento.idea.magento2plugin.project.Settings.getMagentoPath(project);
                if (magentoPath == null || magentoPath.isEmpty()) {
                    response.setMessage("Magento path is not set in the IDE settings. Please configure the Magento path in Settings > Languages & Frameworks > PHP > Magento.");
                } else {
                    response.setMessage("Could not find base directory for vendor: " + request.getPackageName() + ". Magento path is set to: " + magentoPath);
                }
                return response;
            }

            // Use invokeAndWait to ensure the code runs on the EDT before performing write actions
            final List<String>[] generatedFilesHolder = new List[1];
            final Exception[] exceptionHolder = new Exception[1];
            
            try {
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    try {
                        // Generate files in a write action
                        generatedFilesHolder[0] = ApplicationManager.getApplication().runWriteAction(
                            (Computable<List<String>>) () -> {
                                final List<String> files = new ArrayList<>();
                                
                                try {
                                    // Generate composer.json
                                    final String composerPackageName = request.getPackageName().toLowerCase() + "/" + 
                                                                      request.getModuleName().toLowerCase();
                                    final boolean createModuleDirs = true;
                                    
                                    final ModuleComposerJsonData composerData = new ModuleComposerJsonData(
                                        request.getPackageName(),
                                        request.getModuleName(),
                                        baseDir,
                                        request.getModuleDescription(),
                                        composerPackageName,
                                        request.getModuleVersion(),
                                        request.getLicenses(),
                                        request.getDependencies(),
                                        createModuleDirs
                                    );
                                    final ModuleComposerJsonGenerator composerGenerator = new ModuleComposerJsonGenerator(
                                        composerData,
                                        project
                                    );
                                    final PsiFile composerFile = composerGenerator.generate("MCP Module Creation");
                                    if (composerFile != null) {
                                        files.add(composerFile.getVirtualFile().getPath());
                                    }

                                    // Generate registration.php
                                    final ModuleRegistrationPhpData registrationData = new ModuleRegistrationPhpData(
                                        request.getPackageName(),
                                        request.getModuleName(),
                                        baseDir,
                                        createModuleDirs
                                    );
                                    final ModuleRegistrationPhpGenerator registrationGenerator = new ModuleRegistrationPhpGenerator(
                                        registrationData,
                                        project
                                    );
                                    final PsiFile registrationFile = registrationGenerator.generate("MCP Module Creation");
                                    if (registrationFile != null) {
                                        files.add(registrationFile.getVirtualFile().getPath());
                                    }

                                    // Generate module.xml
                                    final List<String> moduleSequences = new ArrayList<>();
                                    final ModuleXmlData moduleXmlData = new ModuleXmlData(
                                        request.getPackageName(),
                                        request.getModuleName(),
                                        request.getModuleVersion(),
                                        baseDir,
                                        moduleSequences,
                                        createModuleDirs
                                    );
                                    final ModuleXmlGenerator moduleXmlGenerator = new ModuleXmlGenerator(
                                        moduleXmlData,
                                        project
                                    );
                                    final PsiFile moduleXmlFile = moduleXmlGenerator.generate("MCP Module Creation");
                                    if (moduleXmlFile != null) {
                                        files.add(moduleXmlFile.getVirtualFile().getPath());
                                    }

                                    // Generate README.md if requested
                                    if (request.isCreateReadme()) {
                                        final ModuleReadmeMdData readmeData = new ModuleReadmeMdData(
                                            request.getPackageName(),
                                            request.getModuleName(),
                                            baseDir
                                        );
                                        final ModuleReadmeMdGenerator readmeGenerator = new ModuleReadmeMdGenerator(
                                            readmeData,
                                            project
                                        );
                                        final PsiFile readmeFile = readmeGenerator.generate("MCP Module Creation");
                                        if (readmeFile != null) {
                                            files.add(readmeFile.getVirtualFile().getPath());
                                        }
                                    }
                                } catch (ProcessCanceledException e) {
                                    // Must rethrow ProcessCanceledException
                                    throw e;
                                } catch (Exception e) {
                                    LOGGER.error("Error generating module files: " + e.getMessage(), e);
                                    exceptionHolder[0] = e;
                                    // Log more detailed information for debugging
                                    LOGGER.debug("Exception details:", e);
                                    LOGGER.debug("Request parameters: packageName=" + request.getPackageName() 
                                        + ", moduleName=" + request.getModuleName());
                                }
                                
                                return files;
                            }
                        );
                    } catch (ProcessCanceledException e) {
                        // Must rethrow ProcessCanceledException
                        throw e;
                    } catch (Exception e) {
                        LOGGER.error("Error in write action: " + e.getMessage(), e);
                        exceptionHolder[0] = e;
                    }
                });
            } catch (ProcessCanceledException e) {
                // Must rethrow ProcessCanceledException
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error invoking on EDT: " + e.getMessage(), e);
                exceptionHolder[0] = e;
            }

            final List<String> generatedFiles = generatedFilesHolder[0];
            
            if (exceptionHolder[0] != null) {
                response.setMessage("Error generating module: " + exceptionHolder[0].getMessage());
                return response;
            }

            if (generatedFiles == null || generatedFiles.isEmpty()) {
                if (exceptionHolder[0] != null) {
                    // Include the specific exception message in the response
                    response.setMessage("Failed to generate module files: " + exceptionHolder[0].getMessage());
                } else {
                    response.setMessage("Failed to generate module files. Check IDE logs for details.");
                }
            } else {
                response.setSuccess(true);
                response.setGeneratedFiles(generatedFiles);
                response.setMessage("Module generated successfully");
            }
        } catch (Exception e) {
            LOGGER.error("Error generating module: " + e.getMessage(), e);
            response.setMessage("Error generating module: " + e.getMessage());
        }

        return response;
    }

    /**
     * Send HTTP response.
     *
     * @param exchange HTTP exchange
     * @param statusCode HTTP status code
     * @param response Response body
     * @throws IOException If an I/O error occurs
     */
    private void sendResponse(
            final HttpExchange exchange,
            final int statusCode,
            final String response
    ) throws IOException {
        final byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}