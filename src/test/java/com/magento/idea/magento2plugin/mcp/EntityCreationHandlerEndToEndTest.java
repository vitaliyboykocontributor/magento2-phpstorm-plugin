/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.magento.idea.magento2plugin.actions.generation.generator.BaseGeneratorTestCase;
import com.magento.idea.magento2plugin.actions.generation.data.dialog.EntityCreatorContextData;
import com.magento.idea.magento2plugin.actions.generation.data.dialog.NewEntityDialogData;
import com.magento.idea.magento2plugin.actions.generation.generator.pool.GeneratorPoolHandler;
import com.magento.idea.magento2plugin.actions.generation.generator.pool.provider.NewEntityGeneratorsProviderUtil;
import com.magento.idea.magento2plugin.actions.generation.context.EntityCreatorContext;
import com.magento.idea.magento2plugin.mcp.model.EntityCreationRequest;
import com.magento.idea.magento2plugin.mcp.model.EntityPropertyData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * END-TO-END test for EntityCreationHandler.
 * Tests complete entity generation flow from AccessLog request to file creation.
 */
public class EntityCreationHandlerEndToEndTest extends BaseGeneratorTestCase {

    /**
     * Test creation of AccessLog entity with exact payload from issue description.
     * Verifies that NewEntityDialogData can be created properly with AccessLog data.
     */
    @Test
    public void testCreateAccessLogEntity() throws Exception {
        // Create AccessLog request from issue description
        final EntityCreationRequest request = createAccessLogRequest();
        
        // Convert request to dialog data using the proper constructor with all 25 parameters
        final NewEntityDialogData dialogData = createDialogDataFromRequest(request);
        
        // Verify the dialog data was created correctly
        assertNotNull("Dialog data should not be null", dialogData);
        assertEquals("Entity name should match", "AccessLog", dialogData.getEntityName());
        assertEquals("Table name should match", "test_accesslog", dialogData.getTableName());
        assertEquals("ID field name should match", "entity_id", dialogData.getIdFieldName());
        assertEquals("Table engine should match", "innodb", dialogData.getTableEngine());
        assertEquals("Table resource should match", "default", dialogData.getTableResource());
        assertTrue("Should have admin UI components", dialogData.hasAdminUiComponents());
        assertTrue("Should have web API", dialogData.hasWebApi());
        assertEquals("Route should match", "test", dialogData.getRoute());
        assertEquals("Menu title should match", "Access Logs", dialogData.getMenuTitle());
        assertEquals("ACL title should match", "Manage Access Logs", dialogData.getAclTitle());
        assertEquals("Properties should match", "entity_id:int,number_of_calls:string,route:string", dialogData.getProperties());
        
        // Test that EntityCreationHandler can be instantiated
        final EntityCreationHandler handler = new EntityCreationHandler(myFixture.getProject());
        assertNotNull("EntityCreationHandler should be created successfully", handler);
    }

    /**
     * Create EntityCreationRequest for AccessLog entity from issue description.
     */
    private EntityCreationRequest createAccessLogRequest() {
        final EntityCreationRequest request = new EntityCreationRequest();
        
        // Basic entity information from issue
        request.setEntityName("AccessLog");
        request.setModuleName("Test_TestModule");
        request.setTableName("test_accesslog");
        request.setIdFieldName("entity_id");
        request.setTableEngine("innodb");
        request.setTableResource("default");
        
        // Feature flags from issue
        request.setHasAdminUiComponents(true);
        request.setHasWebApi(true);
        
        // UI Component settings from issue
        request.setRoute("test");
        request.setMenuId("test_testmodule_accesslog");
        request.setMenuTitle("Access Logs");
        request.setAclId("Test_TestModule::accesslog");
        request.setAclTitle("Manage Access Logs");
        request.setFormName("accesslog_form");
        request.setFormLabel("Access Log Form");
        request.setGridName("accesslog_listing");
        
        // Toolbar settings from issue
        request.setHasToolbar(true);
        request.setHasToolbarBookmarks(true);
        request.setHasToolbarColumnsControl(true);
        request.setHasToolbarListingFilters(true);
        request.setHasToolbarListingPaging(true);
        
        // Entity properties from issue: "entity_id:int,number_of_calls:string,route:string"
        final List<EntityPropertyData> properties = new ArrayList<>();
        properties.add(new EntityPropertyData("entity_id", "int"));
        properties.add(new EntityPropertyData("number_of_calls", "string"));
        properties.add(new EntityPropertyData("route", "string"));
        request.setProperties(properties);
        
        return request;
    }

    /**
     * Convert EntityCreationRequest to NewEntityDialogData using proper constructor.
     * This uses the constructor with all 25 required parameters.
     */
    private NewEntityDialogData createDialogDataFromRequest(final EntityCreationRequest request) {
        // Convert properties to formatted string
        String properties = "";
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            final StringBuilder propertiesBuilder = new StringBuilder();
            for (int i = 0; i < request.getProperties().size(); i++) {
                final EntityPropertyData property = request.getProperties().get(i);
                if (i > 0) {
                    propertiesBuilder.append(",");
                }
                propertiesBuilder.append(property.getName()).append(":").append(property.getType());
            }
            properties = propertiesBuilder.toString();
        }
        
        // Create NewEntityDialogData with all 25 required parameters
        return new NewEntityDialogData(
                request.getEntityName(),                    // entityName
                request.getTableName(),                     // tableName
                request.getIdFieldName(),                   // idFieldName
                request.getTableEngine(),                   // tableEngine
                request.getTableResource(),                 // tableResource
                request.isHasAdminUiComponents(),           // hasAdminUiComponents
                request.isHasDtoInterface(),                // hasDtoInterface
                request.isHasWebApi(),                      // hasWebApi
                request.getRoute(),                         // route
                request.getFormLabel(),                     // formLabel
                request.getFormName(),                      // formName
                request.getGridName(),                      // gridName
                request.isHasToolbar(),                     // hasToolbar
                request.isHasToolbarBookmarks(),            // hasToolbarBookmarks
                request.isHasToolbarColumnsControl(),       // hasToolbarColumnsControl
                request.isHasToolbarListingFilters(),       // hasToolbarListingFilters
                request.isHasToolbarListingPaging(),        // hasToolbarListingPaging
                request.getParentAclId(),                   // parentAclId
                request.getAclId(),                         // aclId
                request.getAclTitle(),                      // aclTitle
                request.getParentMenuId(),                  // parentMenuId
                request.getMenuSortOrder(),                 // menuSortOrder
                request.getMenuId(),                        // menuId
                request.getMenuTitle(),                     // menuTitle
                properties                                  // properties
        );
    }

    /**
     * Verify that a generated file matches the expected template.
     */
    private void verifyGeneratedFile(final String fileName, final String expectedDirectory) {
        // Load expected file from testData
        final String fixturePath = getFixturePath(fileName);
        final PsiFile expectedFile = myFixture.configureByFile(fixturePath);
        
        // Find generated file in project
        final PsiFile generatedFile = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> {
            return myFixture.getPsiManager().findFile(
                myFixture.getProject().getBaseDir().findFileByRelativePath(expectedDirectory + "/" + fileName)
            );
        });
        
        // Verify file exists and matches expected content
        assertNotNull("Generated file should exist: " + expectedDirectory + "/" + fileName, generatedFile);
        assertGeneratedFileIsCorrect(expectedFile, expectedDirectory, generatedFile);
    }
}