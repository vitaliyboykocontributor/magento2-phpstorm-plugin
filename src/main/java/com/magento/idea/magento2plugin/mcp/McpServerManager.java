/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Manager for MCP server lifecycle.
 * Starts and stops the MCP server when the plugin is loaded and unloaded.
 */
@Service(Service.Level.PROJECT)
public final class McpServerManager implements StartupActivity.DumbAware {
    private static final Logger LOGGER = Logger.getInstance(McpServerManager.class);

    /**
     * Start the MCP server when the plugin is loaded.
     *
     * @param project Current project
     */
    @Override
    public void runActivity(@NotNull final Project project) {
        LOGGER.info("Starting MCP server for project: " + project.getName());
        final MagentoMcpServer server = project.getService(MagentoMcpServer.class);
        
        if (server != null) {
            if (server.start()) {
                LOGGER.info("MCP server started on port " + server.getPort());
            } else {
                LOGGER.error("Failed to start MCP server");
            }
        } else {
            LOGGER.error("Failed to get MCP server service");
        }
    }

    /**
     * Stop the MCP server when the plugin is unloaded.
     *
     * @param project Current project
     */
    public static void stopServer(@NotNull final Project project) {
        LOGGER.info("Stopping MCP server for project: " + project.getName());
        final MagentoMcpServer server = project.getService(MagentoMcpServer.class);
        
        if (server != null) {
            server.stop();
            LOGGER.info("MCP server stopped");
        } else {
            LOGGER.error("Failed to get MCP server service");
        }
    }
}