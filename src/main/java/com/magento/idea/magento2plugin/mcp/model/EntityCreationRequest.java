/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for entity creation.
 */
public class EntityCreationRequest {
    // Required basic fields
    private String moduleName;
    private String entityName;
    private String tableName;
    private String idFieldName;
    
    // Optional database settings
    private String tableEngine = "innodb";
    private String tableResource = "default";
    
    // Feature flags
    private boolean hasAdminUiComponents = true;
    private boolean hasDtoInterface = true;
    private boolean hasWebApi = false;
    
    // UI Component settings
    private String route;
    private String formLabel;
    private String formName;
    private String gridName;
    private boolean hasToolbar = true;
    private boolean hasToolbarBookmarks = true;
    private boolean hasToolbarColumnsControl = true;
    private boolean hasToolbarListingFilters = true;
    private boolean hasToolbarListingPaging = true;
    
    // ACL settings
    private String parentAclId = "";
    private String aclId;
    private String aclTitle;
    
    // Menu settings
    private String parentMenuId = "";
    private int menuSortOrder = 100;
    private String menuId;
    private String menuTitle;
    
    // Entity properties (custom fields)
    private List<EntityPropertyData> properties = new ArrayList<>();

    /**
     * Get module name.
     *
     * @return Module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Set module name.
     *
     * @param moduleName Module name
     */
    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Get entity name.
     *
     * @return Entity name
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Set entity name.
     *
     * @param entityName Entity name
     */
    public void setEntityName(final String entityName) {
        this.entityName = entityName;
    }

    /**
     * Get table name.
     *
     * @return Table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Set table name.
     *
     * @param tableName Table name
     */
    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Get ID field name.
     *
     * @return ID field name
     */
    public String getIdFieldName() {
        return idFieldName;
    }

    /**
     * Set ID field name.
     *
     * @param idFieldName ID field name
     */
    public void setIdFieldName(final String idFieldName) {
        this.idFieldName = idFieldName;
    }

    /**
     * Get table engine.
     *
     * @return Table engine
     */
    public String getTableEngine() {
        return tableEngine;
    }

    /**
     * Set table engine.
     *
     * @param tableEngine Table engine
     */
    public void setTableEngine(final String tableEngine) {
        this.tableEngine = tableEngine != null ? tableEngine : "innodb";
    }

    /**
     * Get table resource.
     *
     * @return Table resource
     */
    public String getTableResource() {
        return tableResource;
    }

    /**
     * Set table resource.
     *
     * @param tableResource Table resource
     */
    public void setTableResource(final String tableResource) {
        this.tableResource = tableResource != null ? tableResource : "default";
    }

    /**
     * Check if admin UI components should be generated.
     *
     * @return True if admin UI components should be generated
     */
    public boolean isHasAdminUiComponents() {
        return hasAdminUiComponents;
    }

    /**
     * Set if admin UI components should be generated.
     *
     * @param hasAdminUiComponents True if admin UI components should be generated
     */
    public void setHasAdminUiComponents(final boolean hasAdminUiComponents) {
        this.hasAdminUiComponents = hasAdminUiComponents;
    }

    /**
     * Check if DTO interface should be generated.
     *
     * @return True if DTO interface should be generated
     */
    public boolean isHasDtoInterface() {
        return hasDtoInterface;
    }

    /**
     * Set if DTO interface should be generated.
     *
     * @param hasDtoInterface True if DTO interface should be generated
     */
    public void setHasDtoInterface(final boolean hasDtoInterface) {
        this.hasDtoInterface = hasDtoInterface;
    }

    /**
     * Check if Web API should be generated.
     *
     * @return True if Web API should be generated
     */
    public boolean isHasWebApi() {
        return hasWebApi;
    }

    /**
     * Set if Web API should be generated.
     *
     * @param hasWebApi True if Web API should be generated
     */
    public void setHasWebApi(final boolean hasWebApi) {
        this.hasWebApi = hasWebApi;
    }

    /**
     * Get route.
     *
     * @return Route
     */
    public String getRoute() {
        return route;
    }

    /**
     * Set route.
     *
     * @param route Route
     */
    public void setRoute(final String route) {
        this.route = route;
    }

    /**
     * Get form label.
     *
     * @return Form label
     */
    public String getFormLabel() {
        return formLabel;
    }

    /**
     * Set form label.
     *
     * @param formLabel Form label
     */
    public void setFormLabel(final String formLabel) {
        this.formLabel = formLabel;
    }

    /**
     * Get form name.
     *
     * @return Form name
     */
    public String getFormName() {
        return formName;
    }

    /**
     * Set form name.
     *
     * @param formName Form name
     */
    public void setFormName(final String formName) {
        this.formName = formName;
    }

    /**
     * Get grid name.
     *
     * @return Grid name
     */
    public String getGridName() {
        return gridName;
    }

    /**
     * Set grid name.
     *
     * @param gridName Grid name
     */
    public void setGridName(final String gridName) {
        this.gridName = gridName;
    }

    /**
     * Check if toolbar should be generated.
     *
     * @return True if toolbar should be generated
     */
    public boolean isHasToolbar() {
        return hasToolbar;
    }

    /**
     * Set if toolbar should be generated.
     *
     * @param hasToolbar True if toolbar should be generated
     */
    public void setHasToolbar(final boolean hasToolbar) {
        this.hasToolbar = hasToolbar;
    }

    /**
     * Check if toolbar bookmarks should be generated.
     *
     * @return True if toolbar bookmarks should be generated
     */
    public boolean isHasToolbarBookmarks() {
        return hasToolbarBookmarks;
    }

    /**
     * Set if toolbar bookmarks should be generated.
     *
     * @param hasToolbarBookmarks True if toolbar bookmarks should be generated
     */
    public void setHasToolbarBookmarks(final boolean hasToolbarBookmarks) {
        this.hasToolbarBookmarks = hasToolbarBookmarks;
    }

    /**
     * Check if toolbar columns control should be generated.
     *
     * @return True if toolbar columns control should be generated
     */
    public boolean isHasToolbarColumnsControl() {
        return hasToolbarColumnsControl;
    }

    /**
     * Set if toolbar columns control should be generated.
     *
     * @param hasToolbarColumnsControl True if toolbar columns control should be generated
     */
    public void setHasToolbarColumnsControl(final boolean hasToolbarColumnsControl) {
        this.hasToolbarColumnsControl = hasToolbarColumnsControl;
    }

    /**
     * Check if toolbar listing filters should be generated.
     *
     * @return True if toolbar listing filters should be generated
     */
    public boolean isHasToolbarListingFilters() {
        return hasToolbarListingFilters;
    }

    /**
     * Set if toolbar listing filters should be generated.
     *
     * @param hasToolbarListingFilters True if toolbar listing filters should be generated
     */
    public void setHasToolbarListingFilters(final boolean hasToolbarListingFilters) {
        this.hasToolbarListingFilters = hasToolbarListingFilters;
    }

    /**
     * Check if toolbar listing paging should be generated.
     *
     * @return True if toolbar listing paging should be generated
     */
    public boolean isHasToolbarListingPaging() {
        return hasToolbarListingPaging;
    }

    /**
     * Set if toolbar listing paging should be generated.
     *
     * @param hasToolbarListingPaging True if toolbar listing paging should be generated
     */
    public void setHasToolbarListingPaging(final boolean hasToolbarListingPaging) {
        this.hasToolbarListingPaging = hasToolbarListingPaging;
    }

    /**
     * Get parent ACL ID.
     *
     * @return Parent ACL ID
     */
    public String getParentAclId() {
        return parentAclId;
    }

    /**
     * Set parent ACL ID.
     *
     * @param parentAclId Parent ACL ID
     */
    public void setParentAclId(final String parentAclId) {
        this.parentAclId = parentAclId != null ? parentAclId : "";
    }

    /**
     * Get ACL ID.
     *
     * @return ACL ID
     */
    public String getAclId() {
        return aclId;
    }

    /**
     * Set ACL ID.
     *
     * @param aclId ACL ID
     */
    public void setAclId(final String aclId) {
        this.aclId = aclId;
    }

    /**
     * Get ACL title.
     *
     * @return ACL title
     */
    public String getAclTitle() {
        return aclTitle;
    }

    /**
     * Set ACL title.
     *
     * @param aclTitle ACL title
     */
    public void setAclTitle(final String aclTitle) {
        this.aclTitle = aclTitle;
    }

    /**
     * Get parent menu ID.
     *
     * @return Parent menu ID
     */
    public String getParentMenuId() {
        return parentMenuId;
    }

    /**
     * Set parent menu ID.
     *
     * @param parentMenuId Parent menu ID
     */
    public void setParentMenuId(final String parentMenuId) {
        this.parentMenuId = parentMenuId != null ? parentMenuId : "";
    }

    /**
     * Get menu sort order.
     *
     * @return Menu sort order
     */
    public int getMenuSortOrder() {
        return menuSortOrder;
    }

    /**
     * Set menu sort order.
     *
     * @param menuSortOrder Menu sort order
     */
    public void setMenuSortOrder(final int menuSortOrder) {
        this.menuSortOrder = menuSortOrder;
    }

    /**
     * Get menu ID.
     *
     * @return Menu ID
     */
    public String getMenuId() {
        return menuId;
    }

    /**
     * Set menu ID.
     *
     * @param menuId Menu ID
     */
    public void setMenuId(final String menuId) {
        this.menuId = menuId;
    }

    /**
     * Get menu title.
     *
     * @return Menu title
     */
    public String getMenuTitle() {
        return menuTitle;
    }

    /**
     * Set menu title.
     *
     * @param menuTitle Menu title
     */
    public void setMenuTitle(final String menuTitle) {
        this.menuTitle = menuTitle;
    }

    /**
     * Get entity properties.
     *
     * @return Entity properties
     */
    public List<EntityPropertyData> getProperties() {
        return properties;
    }

    /**
     * Set entity properties.
     *
     * @param properties Entity properties
     */
    public void setProperties(final List<EntityPropertyData> properties) {
        this.properties = properties != null ? properties : new ArrayList<>();
    }
}