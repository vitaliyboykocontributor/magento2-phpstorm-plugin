package com.magento.idea.magento2plugin.mcp.model;


/**
 * Property data class.
 */
public class PropertyData {
    private String name;
    private String type;

    /**
     * Default constructor.
     */
    public PropertyData() {
    }

    /**
     * Constructor.
     *
     * @param name Property name
     * @param type Property type
     */
    public PropertyData(final String name, final String type) {
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