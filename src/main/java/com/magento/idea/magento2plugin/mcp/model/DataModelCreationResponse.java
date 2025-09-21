/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for data model creation.
 */
public class DataModelCreationResponse {
    private boolean success;
    private String message;
    private List<String> generatedFiles = new ArrayList<>();

    /**
     * Default constructor.
     */
    public DataModelCreationResponse() {
    }

    /**
     * Constructor.
     *
     * @param success True if successful, false otherwise
     * @param message Response message
     * @param generatedFiles List of generated files
     */
    public DataModelCreationResponse(final boolean success, final String message, final List<String> generatedFiles) {
        this.success = success;
        this.message = message;
        this.generatedFiles = generatedFiles != null ? generatedFiles : new ArrayList<>();
    }

    /**
     * Check if the operation was successful.
     *
     * @return True if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Set if the operation was successful.
     *
     * @param success True if successful, false otherwise
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * Get the response message.
     *
     * @return Response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the response message.
     *
     * @param message Response message
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Get the list of generated files.
     *
     * @return List of generated files
     */
    public List<String> getGeneratedFiles() {
        return generatedFiles;
    }

    /**
     * Set the list of generated files.
     *
     * @param generatedFiles List of generated files
     */
    public void setGeneratedFiles(final List<String> generatedFiles) {
        this.generatedFiles = generatedFiles != null ? generatedFiles : new ArrayList<>();
    }

    /**
     * Convert response to JSON string.
     *
     * @return JSON string representation
     */
    public String toJson() {
        final JSONObject json = new JSONObject();
        json.put("success", success);
        json.put("message", message);
        
        final JSONArray filesArray = new JSONArray();
        for (final String file : generatedFiles) {
            filesArray.put(file);
        }
        json.put("generatedFiles", filesArray);
        
        return json.toString();
    }
}