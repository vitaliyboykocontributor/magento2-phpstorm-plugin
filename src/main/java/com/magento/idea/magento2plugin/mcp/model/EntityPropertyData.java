/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.model;

/**
 * Data model for entity property.
 */
public class EntityPropertyData {
    private String name;
    private String type;

    /**
     * Default constructor.
     */
    public EntityPropertyData() {
        // Empty constructor for JSON deserialization
    }

    /**
     * Constructor with parameters.
     *
     * @param name Property name
     * @param type Property type
     */
    public EntityPropertyData(final String name, final String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Get property name.
     *
     * @return Property name
     */
    public String getName() {
        return name;
    }

    /**
     * Set property name.
     *
     * @param name Property name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get property type.
     *
     * @return Property type
     */
    public String getType() {
        return type;
    }

    /**
     * Set property type.
     *
     * @param type Property type
     */
    public void setType(final String type) {
        this.type = type;
    }
}