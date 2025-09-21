/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing formatted properties strings used by both 
 * EntityCreationHandler and DataModelCreationHandler.
 */
public final class PropertyParsingUtil {

    private PropertyParsingUtil() {}

    /**
     * Parse formatted properties string back to short properties format.
     * The formatted string contains properties in format: "UPPER_SNAKE;lower_snake;type;UpperCamel;lowerCamel"
     * separated by commas.
     *
     * @param formattedPropertiesString Formatted properties string from ClassPropertyFormatterUtil
     * @return List of short property maps with Name and Type keys
     */
    public static List<Map<String, String>> parseFormattedPropertiesString(final String formattedPropertiesString) {
        final List<Map<String, String>> shortProperties = new ArrayList<>();
        
        if (formattedPropertiesString == null || formattedPropertiesString.trim().isEmpty()) {
            return shortProperties;
        }
        
        // Split by comma to get individual property strings
        final String[] propertyStrings = formattedPropertiesString.split(",");
        
        for (final String propertyString : propertyStrings) {
            if (propertyString.trim().isEmpty()) {
                continue;
            }
            
            // Each property string is in format: "UPPER_SNAKE;lower_snake;type;UpperCamel;lowerCamel"
            final String[] parts = propertyString.split(";");
            if (parts.length >= 3) {
                final String lowerSnakeName = parts[1]; // lower_snake format (database column name)
                final String type = parts[2]; // property type
                
                final Map<String, String> shortProperty = new HashMap<>();
                shortProperty.put("Name", lowerSnakeName);
                shortProperty.put("Type", type);
                shortProperties.add(shortProperty);
            }
        }
        
        return shortProperties;
    }
}