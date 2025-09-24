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
import com.magento.idea.magento2plugin.actions.generation.NewDataModelAction;
import com.magento.idea.magento2plugin.actions.generation.OverrideClassByAPreferenceAction;
import com.magento.idea.magento2plugin.actions.generation.data.DataModelData;
import com.magento.idea.magento2plugin.actions.generation.data.DataModelInterfaceData;
import com.magento.idea.magento2plugin.actions.generation.data.PreferenceDiXmFileData;
import com.magento.idea.magento2plugin.actions.generation.dialog.util.ClassPropertyFormatterUtil;
import com.magento.idea.magento2plugin.actions.generation.generator.DataModelGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.DataModelInterfaceGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.PreferenceDiXmlGenerator;
import com.magento.idea.magento2plugin.magento.files.DataModelFile;
import com.magento.idea.magento2plugin.magento.files.DataModelInterfaceFile;
import com.magento.idea.magento2plugin.mcp.model.DataModelCreationRequest;
import com.magento.idea.magento2plugin.mcp.model.DataModelCreationResponse;
import com.magento.idea.magento2plugin.mcp.model.PropertyData;
import com.magento.idea.magento2plugin.mcp.util.JsonUtil;
import com.magento.idea.magento2plugin.mcp.util.McpPathUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for data model creation requests from MCP server.
 */
public class DataModelCreationHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getInstance(DataModelCreationHandler.class);
    private final Project project;

    /**
     * Constructor.
     * 
     * @param project The IntelliJ project
     */
    public DataModelCreationHandler(final @NotNull Project project) {
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
            final DataModelCreationRequest request = parseDataModelCreationRequest(requestString);

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

            // Generate data model
            final DataModelCreationResponse response = generateDataModel(request);
            
            // Send response
            final String responseJson = JsonUtil.toJson(response);
            sendResponse(exchange, 200, responseJson);
        } catch (Exception e) {
            LOGGER.error("Error handling data model creation request: " + e.getMessage(), e);
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Parse JSON string into DataModelCreationRequest with proper PropertyData conversion.
     *
     * @param jsonString JSON string to parse
     * @return DataModelCreationRequest or null if parsing fails
     */
    private DataModelCreationRequest parseDataModelCreationRequest(final String jsonString) {
        try {
            final JSONObject jsonObject = new JSONObject(jsonString);
            final DataModelCreationRequest request = new DataModelCreationRequest();
            
            // Parse basic fields
            if (jsonObject.has("moduleName")) {
                request.setModuleName(jsonObject.getString("moduleName"));
            }
            if (jsonObject.has("modelName")) {
                request.setModelName(jsonObject.getString("modelName"));
            }
            if (jsonObject.has("createInterface")) {
                request.setCreateInterface(jsonObject.getBoolean("createInterface"));
            }
            
            // Parse properties field (handle both string and array formats)
            if (jsonObject.has("properties")) {
                final Object propertiesValue = jsonObject.get("properties");
                final List<PropertyData> properties = new ArrayList<>();
                
                if (propertiesValue instanceof String) {
                    // Handle string format: "name:type,name:type,..."
                    final String propertiesString = (String) propertiesValue;
                    final String[] propertyPairs = propertiesString.split(",");
                    
                    for (final String pair : propertyPairs) {
                        final String trimmedPair = pair.trim();
                        if (!trimmedPair.isEmpty()) {
                            final String[] parts = trimmedPair.split(":");
                            if (parts.length == 2) {
                                final String name = parts[0].trim();
                                final String type = parts[1].trim();
                                properties.add(new PropertyData(name, type));
                            }
                        }
                    }
                } else if (propertiesValue instanceof JSONArray) {
                    // Handle array format: [{"name": "field_name", "type": "string"}, ...]
                    final JSONArray propertiesArray = (JSONArray) propertiesValue;
                    
                    for (int i = 0; i < propertiesArray.length(); i++) {
                        final Object item = propertiesArray.get(i);
                        if (item instanceof JSONObject) {
                            final JSONObject propertyObj = (JSONObject) item;
                            if (propertyObj.has("name") && propertyObj.has("type")) {
                                final String name = propertyObj.getString("name");
                                final String type = propertyObj.getString("type");
                                properties.add(new PropertyData(name, type));
                            }
                        }
                    }
                }
                
                request.setProperties(properties);
            }
            
            return request;
        } catch (Exception e) {
            LOGGER.error("Error parsing DataModelCreationRequest JSON: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate the data model creation request.
     *
     * @param request Data model creation request
     * @return Validation error message or null if valid
     */
    private String validateRequest(final DataModelCreationRequest request) {
        if (request.getModuleName() == null || request.getModuleName().isEmpty()) {
            return "Module name is required";
        }
        if (request.getModelName() == null || request.getModelName().isEmpty()) {
            return "Model name is required";
        }
        if (request.getProperties() == null || request.getProperties().isEmpty()) {
            return "At least one property is required for the data model";
        }
        
        // Validate property names (lower_snake_case pattern)
        final String lowerSnakeCasePattern = "^[a-z][a-z0-9]*(_[a-z0-9]+)*$";
        for (final PropertyData property : request.getProperties()) {
            if (property.getName() == null || property.getName().trim().isEmpty()) {
                return "Property name cannot be empty";
            }
            
            if (!property.getName().matches(lowerSnakeCasePattern)) {
                return "Property name '" + property.getName() + "' must be in lower_snake_case format";
            }
        }
        
        return null;
    }

    /**
     * Generate the data model files.
     *
     * @param request Data model creation request
     * @return Data model creation response
     */
    private DataModelCreationResponse generateDataModel(final DataModelCreationRequest request) {
        final DataModelCreationResponse response = new DataModelCreationResponse();
        response.setSuccess(false);

        try {
            // Find the module directory
            final PsiDirectory moduleDirectory = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiDirectory>) () -> McpPathUtil.findModuleDirectory(project, request.getModuleName())
            );

            if (moduleDirectory == null) {
                response.setMessage("Module directory not found for module: " + request.getModuleName());
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
                                    // Format properties using ClassPropertyFormatterUtil
                                    final List<String> formattedProperties = new ArrayList<>();
                                    for (final PropertyData property : request.getProperties()) {
                                        final String formatted = ClassPropertyFormatterUtil.formatSingleProperty(
                                            property.getName(),
                                            property.getType()
                                        );
                                        formattedProperties.add(formatted);
                                    }
                                    final String propertiesString = ClassPropertyFormatterUtil.joinProperties(formattedProperties);
                                    
                                    // Generate interface name
                                    final String interfaceName = request.getModelName() + "Interface";

                                    // Generate data model file (always generated)
                                    final DataModelData modelData = new DataModelData(
                                        request.getModelName(),
                                        interfaceName,
                                        request.getModuleName(),
                                        propertiesString,
                                        request.isCreateInterface()
                                    );
                                    
                                    final DataModelGenerator modelGenerator = new DataModelGenerator(project, modelData);
                                    final PsiFile modelFile = modelGenerator.generate(NewDataModelAction.ACTION_NAME, true);
                                    
                                    if (modelFile != null) {
                                        files.add(modelFile.getVirtualFile().getPath());
                                    }

                                    // Generate interface file and preference (if requested)
                                    if (request.isCreateInterface()) {
                                        // Generate interface file
                                        final DataModelInterfaceData interfaceData = new DataModelInterfaceData(
                                            interfaceName,
                                            request.getModuleName(),
                                            propertiesString
                                        );
                                        
                                        final DataModelInterfaceGenerator interfaceGenerator = 
                                            new DataModelInterfaceGenerator(interfaceData, project);
                                        final PsiFile interfaceFile = interfaceGenerator.generate(NewDataModelAction.ACTION_NAME, true);
                                        
                                        if (interfaceFile != null) {
                                            files.add(interfaceFile.getVirtualFile().getPath());
                                        }

                                        // Generate DI preference
                                        final PreferenceDiXmFileData preferenceData = new PreferenceDiXmFileData(
                                            request.getModuleName(),
                                            new DataModelInterfaceFile(request.getModuleName(), interfaceName).getClassFqn(),
                                            new DataModelFile(request.getModuleName(), request.getModelName()).getClassFqn(),
                                            "base"
                                        );
                                        
                                        final PreferenceDiXmlGenerator preferenceGenerator = 
                                            new PreferenceDiXmlGenerator(preferenceData, project);
                                        final PsiFile preferenceFile = preferenceGenerator.generate(OverrideClassByAPreferenceAction.ACTION_NAME);
                                        
                                        if (preferenceFile != null) {
                                            files.add(preferenceFile.getVirtualFile().getPath());
                                        }
                                    }
                                } catch (ProcessCanceledException e) {
                                    // Must rethrow ProcessCanceledException
                                    throw e;
                                } catch (Exception e) {
                                    LOGGER.error("Error generating data model files: " + e.getMessage(), e);
                                    exceptionHolder[0] = e;
                                    // Log more detailed information for debugging
                                    LOGGER.debug("Exception details:", e);
                                    LOGGER.debug("Request parameters: moduleName=" + request.getModuleName() 
                                        + ", modelName=" + request.getModelName());
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
                response.setMessage("Error generating data model: " + exceptionHolder[0].getMessage());
                return response;
            }

            if (generatedFiles == null || generatedFiles.isEmpty()) {
                if (exceptionHolder[0] != null) {
                    // Include the specific exception message in the response
                    response.setMessage("Failed to generate data model files: " + exceptionHolder[0].getMessage());
                } else {
                    response.setMessage("Failed to generate data model files. Check IDE logs for details.");
                }
            } else {
                response.setSuccess(true);
                response.setGeneratedFiles(generatedFiles);
                response.setMessage("Data model created successfully");
            }
        } catch (Exception e) {
            LOGGER.error("Error generating data model: " + e.getMessage(), e);
            response.setMessage("Error generating data model: " + e.getMessage());
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