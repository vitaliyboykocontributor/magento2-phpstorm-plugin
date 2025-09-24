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
import com.magento.idea.magento2plugin.actions.generation.data.dialog.EntityCreatorContextData;
import com.magento.idea.magento2plugin.actions.generation.data.dialog.NewEntityDialogData;
import com.magento.idea.magento2plugin.actions.generation.dialog.util.ClassPropertyFormatterUtil;
import com.magento.idea.magento2plugin.actions.generation.generator.pool.GeneratorPoolHandler;
import com.magento.idea.magento2plugin.actions.generation.generator.pool.provider.NewEntityGeneratorsProviderUtil;
import com.magento.idea.magento2plugin.actions.generation.context.EntityCreatorContext;
import com.magento.idea.magento2plugin.actions.generation.util.GenerationContextRegistry;
import com.magento.idea.magento2plugin.mcp.model.EntityCreationRequest;
import com.magento.idea.magento2plugin.mcp.model.EntityCreationResponse;
import com.magento.idea.magento2plugin.mcp.model.PropertyData;
import com.magento.idea.magento2plugin.mcp.util.JsonUtil;
import com.magento.idea.magento2plugin.mcp.util.McpPathUtil;
import com.magento.idea.magento2plugin.mcp.util.PropertyParsingUtil;
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
 * Handler for entity creation requests.
 */
public class EntityCreationHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getInstance(EntityCreationHandler.class);
    private final Project project;

    /**
     * Constructor.
     *
     * @param project Current project
     */
    public EntityCreationHandler(@NotNull final Project project) {
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
            final EntityCreationRequest request = JsonUtil.fromJson(requestString, EntityCreationRequest.class);

            final JSONObject jsonObject = new JSONObject(requestString);
            // Convert properties from EntityPropertyData to the string format expected by NewEntityDialogData
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

            // Generate entity
            final EntityCreationResponse response = generateEntity(request);
            
            // Send response
            final String responseJson = JsonUtil.toJson(response);
            sendResponse(exchange, 200, responseJson);
        } catch (Exception e) {
            LOGGER.error("Error handling entity creation request: " + e.getMessage(), e);
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Validate the entity creation request.
     *
     * @param request Entity creation request
     * @return Validation error message or null if valid
     */
    private String validateRequest(final EntityCreationRequest request) {
        if (request.getModuleName() == null || request.getModuleName().isEmpty()) {
            return "Module name is required";
        }
        if (request.getEntityName() == null || request.getEntityName().isEmpty()) {
            return "Entity name is required";
        }
        if (request.getTableName() == null || request.getTableName().isEmpty()) {
            return "Table name is required";
        }
        if (request.getIdFieldName() == null || request.getIdFieldName().isEmpty()) {
            return "ID field name is required";
        }
        
        // Validate UI component fields if admin UI components are enabled
        if (request.isHasAdminUiComponents()) {
            if (request.getRoute() == null || request.getRoute().isEmpty()) {
                return "Route is required when admin UI components are enabled";
            }
            if (request.getFormName() == null || request.getFormName().isEmpty()) {
                return "Form name is required when admin UI components are enabled";
            }
            if (request.getGridName() == null || request.getGridName().isEmpty()) {
                return "Grid name is required when admin UI components are enabled";
            }
            if (request.getAclId() == null || request.getAclId().isEmpty()) {
                return "ACL ID is required when admin UI components are enabled";
            }
            if (request.getMenuId() == null || request.getMenuId().isEmpty()) {
                return "Menu ID is required when admin UI components are enabled";
            }
        }
        
        return null;
    }

    /**
     * Generate the entity files.
     *
     * @param request Entity creation request
     * @return Entity creation response
     */
    private EntityCreationResponse generateEntity(final EntityCreationRequest request) {
        final EntityCreationResponse response = new EntityCreationResponse();
        response.setSuccess(false);

        try {
            // Find the base directory for entity creation
            final PsiDirectory baseDir = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiDirectory>) () -> McpPathUtil.findModuleDirectory(project, request.getModuleName())
            );

            if (baseDir == null) {
                String magentoPath = com.magento.idea.magento2plugin.project.Settings.getMagentoPath(project);
                if (magentoPath == null || magentoPath.isEmpty()) {
                    response.setMessage("Magento path is not set in the IDE settings. Please configure the Magento path in Settings > Languages & Frameworks > PHP > Magento.");
                } else {
                    response.setMessage("Could not find module directory for module: " + request.getModuleName() + ". Magento path is set to: " + magentoPath);
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
                                    // Convert EntityCreationRequest to NewEntityDialogData
                                    final NewEntityDialogData dialogData = convertToNewEntityDialogData(request);
                    
                                    // Create EntityCreatorContextData using the same logic as NewEntityDialog
                                    final EntityCreatorContextData context = createEntityCreatorContextData(dialogData, request.getModuleName(), request);
                                    
                                    // Set up generation context
                                    final EntityCreatorContext generationContext = new EntityCreatorContext();
                                    generationContext.putUserData(
                                            EntityCreatorContext.DTO_TYPE,
                                            dialogData.hasDtoInterface()
                                                    ? context.getDtoInterfaceNamespaceBuilder().getClassFqn()
                                                    : context.getDtoModelNamespaceBuilder().getClassFqn()
                                    );
                                    generationContext.putUserData(EntityCreatorContext.ENTITY_ID, dialogData.getIdFieldName());
                                    GenerationContextRegistry.getInstance().setContext(generationContext);

                                    // Initialize and run the generator pool
                                    final GeneratorPoolHandler generatorPoolHandler = new GeneratorPoolHandler(context);
                                    NewEntityGeneratorsProviderUtil.initializeGenerators(
                                            generatorPoolHandler,
                                            context,
                                            dialogData
                                    );
                                    
                                    generatorPoolHandler.run();
                                    
                                    // TODO: Collect generated file paths from the generators
                                    // For now, return a success message indicating generation completed
                                    files.add("Entity generation completed successfully");
                                    
                                } catch (ProcessCanceledException e) {
                                    // Must rethrow ProcessCanceledException
                                    throw e;
                                } catch (Exception e) {
                                    LOGGER.error("Error generating entity files: " + e.getMessage(), e);
                                    exceptionHolder[0] = e;
                                    // Log more detailed information for debugging
                                    LOGGER.debug("Exception details:", e);
                                    LOGGER.debug("Request parameters: moduleName=" + request.getModuleName() 
                                        + ", entityName=" + request.getEntityName());
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
                response.setMessage("Error generating entity: " + exceptionHolder[0].getMessage());
                return response;
            }

            if (generatedFiles == null || generatedFiles.isEmpty()) {
                if (exceptionHolder[0] != null) {
                    // Include the specific exception message in the response
                    response.setMessage("Failed to generate entity files: " + exceptionHolder[0].getMessage());
                } else {
                    response.setMessage("Failed to generate entity files. Check IDE logs for details.");
                }
            } else {
                response.setSuccess(true);
                response.setGeneratedFiles(generatedFiles);
                response.setMessage("Entity generated successfully");
            }
        } catch (Exception e) {
            LOGGER.error("Error generating entity: " + e.getMessage(), e);
            response.setMessage("Error generating entity: " + e.getMessage());
        }

        return response;
    }

    /**
     * Convert EntityCreationRequest to NewEntityDialogData.
     *
     * @param request Entity creation request
     * @return NewEntityDialogData
     */
    private NewEntityDialogData convertToNewEntityDialogData(final EntityCreationRequest request) {
        // Convert properties from EntityPropertyData to the string format expected by NewEntityDialogData
        final List<String> formattedProperties = new ArrayList<>();
        for (final PropertyData property : request.getProperties()) {
            final String formatted = ClassPropertyFormatterUtil.formatSingleProperty(
                property.getName(), 
                property.getType()
            );
            formattedProperties.add(formatted);
        }
        final String propertiesString = ClassPropertyFormatterUtil.joinProperties(formattedProperties);

        return new NewEntityDialogData(
            request.getEntityName(),
            request.getTableName(),
            request.getIdFieldName(),
            request.getTableEngine(),
            request.getTableResource(),
            request.isHasAdminUiComponents(),
            request.isHasDtoInterface(),
            request.isHasWebApi(),
            request.getRoute(),
            request.getFormLabel(),
            request.getFormName(),
            request.getGridName(),
            request.isHasToolbar(),
            request.isHasToolbarBookmarks(),
            request.isHasToolbarColumnsControl(),
            request.isHasToolbarListingFilters(),
            request.isHasToolbarListingPaging(),
            request.getParentAclId(),
            request.getAclId(),
            request.getAclTitle(),
            request.getParentMenuId(),
            request.getMenuSortOrder(),
            request.getMenuId(),
            request.getMenuTitle(),
            propertiesString
        );
    }

    /**
     * Create EntityCreatorContextData using the same logic as NewEntityDialog.getEntityCreatorContextData().
     *
     * @param dialogData NewEntityDialogData
     * @param moduleName Module name from the original request
     * @param request EntityCreationRequest
     * @return EntityCreatorContextData
     */
    private EntityCreatorContextData createEntityCreatorContextData(final NewEntityDialogData dialogData, final String moduleName, final EntityCreationRequest request) {
        final String entityName = dialogData.getEntityName();
        final String dtoModelSuffix = "Data";
        final String dtoInterfaceSuffix = "Interface";
        final String dtoClassName = entityName.concat(dtoModelSuffix);
        final String dtoInterfaceClassName = entityName.concat(dtoInterfaceSuffix);
        final String actionName = "Create Entity";
        final boolean openFilesFlag = false;

        final String actionsPathPrefix = dialogData.getRoute() + com.magento.idea.magento2plugin.magento.packages.File.separator
                + entityName.toLowerCase(java.util.Locale.getDefault()) + com.magento.idea.magento2plugin.magento.packages.File.separator;
        
        // Create namespace builders
        final com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder dtoModelNamespace =
                new com.magento.idea.magento2plugin.magento.files.DataModelFile(moduleName, dtoClassName).getNamespaceBuilder();
        final com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder dtoInterfaceNamespace =
                new com.magento.idea.magento2plugin.magento.files.DataModelInterfaceFile(moduleName, dtoInterfaceClassName).getNamespaceBuilder();

        final com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder formViewNamespaceBuilder =
                new com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder(
                        moduleName,
                        "Edit",
                        com.magento.idea.magento2plugin.magento.files.ControllerBackendPhp.DEFAULT_DIR + com.magento.idea.magento2plugin.magento.packages.File.separator + entityName
                );

        return new EntityCreatorContextData(
                project,
                moduleName,
                actionName,
                openFilesFlag,
                dialogData.hasWebApi(),
                actionsPathPrefix.concat("index"),
                actionsPathPrefix.concat("edit"),
                actionsPathPrefix.concat("new"),
                actionsPathPrefix.concat("delete"),
                dtoModelNamespace,
                dtoInterfaceNamespace,
                formViewNamespaceBuilder,
                new com.magento.idea.magento2plugin.magento.files.actions.NewActionFile(moduleName, entityName).getNamespaceBuilder(),
                createEntityProperties(dialogData, request),
                createButtons(dialogData, moduleName),
                createFieldSets(),
                createFields(dialogData)
        );
    }

    /**
     * Create entity properties list using the same logic as NewEntityDialog.
     * Parse the formatted properties string from dialogData instead of bypassing it.
     *
     * @param dialogData NewEntityDialogData
     * @param request EntityCreationRequest
     * @return List of entity properties
     */
    private java.util.List<java.util.Map<String, String>> createEntityProperties(final NewEntityDialogData dialogData, final EntityCreationRequest request) {
        // Parse the formatted properties string from dialogData back to short properties format
        final java.util.List<java.util.Map<String, String>> shortProperties = PropertyParsingUtil.parseFormattedPropertiesString(dialogData.getProperties());
        
        // Use DbSchemaGeneratorUtil to complement properties with proper database column metadata
        final java.util.List<java.util.Map<String, String>> complementedProperties = 
                com.magento.idea.magento2plugin.actions.generation.generator.util.DbSchemaGeneratorUtil
                        .complementShortPropertiesByDefaults(shortProperties);
        
        // Add the identity column at the beginning (same as NewEntityDialog)
        complementedProperties.add(0, 
                com.magento.idea.magento2plugin.actions.generation.generator.util.DbSchemaGeneratorUtil
                        .getTableIdentityColumnData(dialogData.getIdFieldName()));
        
        return complementedProperties;
    }

    /**
     * Create form buttons list.
     *
     * @param dialogData NewEntityDialogData
     * @param moduleName Module name
     * @return List of form buttons
     */
    private java.util.List<com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormButtonData> createButtons(final NewEntityDialogData dialogData, final String moduleName) {
        final java.util.List<com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormButtonData> buttons = new java.util.ArrayList<>();
        final String entityName = dialogData.getEntityName();
        final String directory = "Block/Form/" + entityName;

        // Save button
        final com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder namespaceBuilderSave = 
                new com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder(
                        moduleName,
                        "Save",
                        directory
                );
        buttons.add(new com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormButtonData(
                directory,
                "Save",
                moduleName,
                "Save",
                namespaceBuilderSave.getNamespace(),
                "Save Entity",
                "10",
                dialogData.getFormName(),
                namespaceBuilderSave.getClassFqn()
        ));

        // Back button
        final com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder namespaceBuilderBack = 
                new com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder(
                        moduleName,
                        "Back",
                        directory
                );
        buttons.add(new com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormButtonData(
                directory,
                "Back",
                moduleName,
                "Back",
                namespaceBuilderBack.getNamespace(),
                "Back To Grid",
                "20",
                dialogData.getFormName(),
                namespaceBuilderBack.getClassFqn()
        ));

        // Delete button
        final com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder namespaceBuilderDelete = 
                new com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder(
                        moduleName,
                        "Delete",
                        directory
                );
        buttons.add(new com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormButtonData(
                directory,
                "Delete",
                moduleName,
                "Delete",
                namespaceBuilderDelete.getNamespace(),
                "Delete Entity",
                "30",
                dialogData.getFormName(),
                namespaceBuilderDelete.getClassFqn()
        ));

        return buttons;
    }

    /**
     * Create form fieldsets.
     *
     * @return List of form fieldsets
     */
    private java.util.List<com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldsetData> createFieldSets() {
        final java.util.List<com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldsetData> fieldSets = new java.util.ArrayList<>();
        final com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldsetData fieldsetData = 
                new com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldsetData(
                        "general",
                        "General",
                        "10"
                );
        fieldSets.add(fieldsetData);
        return fieldSets;
    }

    /**
     * Create form fields.
     *
     * @param dialogData NewEntityDialogData
     * @return List of form fields
     */
    private java.util.List<com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldData> createFields(final NewEntityDialogData dialogData) {
        final java.util.List<com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldData> fields = new java.util.ArrayList<>();

        // Add ID field (hidden)
        fields.add(new com.magento.idea.magento2plugin.actions.generation.data.UiComponentFormFieldData(
                dialogData.getIdFieldName(),
                "Entity ID",
                "0",
                "general",
                com.magento.idea.magento2plugin.magento.packages.uicomponent.FormElementType.HIDDEN.getType(),
                "text",
                dialogData.getIdFieldName()
        ));

        // For now, we'll create a minimal set of fields
        // The full implementation would parse the properties string and create fields for each property
        
        return fields;
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