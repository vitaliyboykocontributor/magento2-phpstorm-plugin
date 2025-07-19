/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Project manager listener for MCP server.
 * Stops the MCP server when the project is closed.
 */
public class McpProjectManagerListener implements ProjectManagerListener {
    private static final Logger LOGGER = Logger.getInstance(McpProjectManagerListener.class);

    /**
     * Called when a project is being closed.
     * Stops the MCP server for the project.
     *
     * @param project Project being closed
     */
    @Override
    public void projectClosing(@NotNull final Project project) {
        LOGGER.info("Project closing, stopping MCP server for project: " + project.getName());
        McpServerManager.stopServer(project);
    }
}