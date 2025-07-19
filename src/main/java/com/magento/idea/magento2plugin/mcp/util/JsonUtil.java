/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.util;

import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for JSON serialization/deserialization.
 */
public final class JsonUtil {
    private static final Logger LOGGER = Logger.getInstance(JsonUtil.class);

    private JsonUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert an object to a JSON string.
     *
     * @param object Object to convert
     * @return JSON string
     */
    public static String toJson(final Object object) {
        if (object == null) {
            return "{}";
        }

        try {
            final JSONObject jsonObject = new JSONObject();
            final Field[] fields = object.getClass().getDeclaredFields();

            for (final Field field : fields) {
                field.setAccessible(true);
                final Object value = field.get(object);
                
                if (value != null) {
                    if (value instanceof List) {
                        final JSONArray jsonArray = new JSONArray();
                        for (final Object item : (List<?>) value) {
                            jsonArray.put(item);
                        }
                        jsonObject.put(field.getName(), jsonArray);
                    } else {
                        jsonObject.put(field.getName(), value);
                    }
                }
            }

            return jsonObject.toString();
        } catch (Exception e) {
            LOGGER.error("Error converting object to JSON: " + e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Convert a JSON string to an object.
     *
     * @param json JSON string
     * @param clazz Class of the object to create
     * @param <T> Type of the object to create
     * @return Object of type T
     */
    public static <T> T fromJson(final String json, final Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(json);
            final T object = clazz.getDeclaredConstructor().newInstance();
            final Field[] fields = clazz.getDeclaredFields();

            for (final Field field : fields) {
                field.setAccessible(true);
                final String fieldName = field.getName();

                if (jsonObject.has(fieldName) && !jsonObject.isNull(fieldName)) {
                    final Object value = jsonObject.get(fieldName);
                    
                    if (value instanceof JSONArray && List.class.isAssignableFrom(field.getType())) {
                        final JSONArray jsonArray = (JSONArray) value;
                        final List<Object> list = new ArrayList<>();
                        
                        for (int i = 0; i < jsonArray.length(); i++) {
                            list.add(jsonArray.get(i));
                        }
                        
                        field.set(object, list);
                    } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        field.set(object, jsonObject.getBoolean(fieldName));
                    } else if (field.getType() == int.class || field.getType() == Integer.class) {
                        field.set(object, jsonObject.getInt(fieldName));
                    } else if (field.getType() == long.class || field.getType() == Long.class) {
                        field.set(object, jsonObject.getLong(fieldName));
                    } else if (field.getType() == double.class || field.getType() == Double.class) {
                        field.set(object, jsonObject.getDouble(fieldName));
                    } else if (field.getType() == String.class) {
                        field.set(object, jsonObject.getString(fieldName));
                    }
                }
            }

            return object;
        } catch (JSONException e) {
            LOGGER.error("Error parsing JSON: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Error converting JSON to object: " + e.getMessage(), e);
            return null;
        }
    }
}