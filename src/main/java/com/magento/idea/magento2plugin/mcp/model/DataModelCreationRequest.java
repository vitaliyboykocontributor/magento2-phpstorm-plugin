/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for data model creation.
 */
public class DataModelCreationRequest {
    private String moduleName;
    private String modelName;
    private boolean createInterface = false;
    private List<PropertyData> properties = new ArrayList<>();

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
     * Get model name.
     *
     * @return Model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Set model name.
     *
     * @param modelName Model name
     */
    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    /**
     * Check if interface should be created.
     *
     * @return True if interface should be created, false otherwise
     */
    public boolean isCreateInterface() {
        return createInterface;
    }

    /**
     * Set if interface should be created.
     *
     * @param createInterface True if interface should be created, false otherwise
     */
    public void setCreateInterface(final boolean createInterface) {
        this.createInterface = createInterface;
    }

    /**
     * Get properties.
     *
     * @return List of properties
     */
    public List<PropertyData> getProperties() {
        return properties;
    }

    /**
     * Set properties.
     *
     * @param properties List of properties
     */
    public void setProperties(final List<PropertyData> properties) {
        this.properties = properties != null ? properties : new ArrayList<>();
    }
}