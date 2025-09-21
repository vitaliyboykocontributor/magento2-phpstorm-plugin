/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * MCP (Model Context Protocol) client for Magento code generation.
 * This server allows AI agents to interact with the Magento code generation capabilities.
 */
@Service(Service.Level.PROJECT)
public final class MagentoMcpClient {
    private static final Logger LOGGER = Logger.getInstance(MagentoMcpClient.class);
    private static final int DEFAULT_PORT = 8090;
    private static final int DEFAULT_BACKLOG = 0;
    private static final String SERVER_NAME = "Magento MCP Server";

    private final Project project;
    private HttpServer client;
    private int port = DEFAULT_PORT;
    private boolean isRunning = false;

    /**
     * Constructor.
     *
     * @param project Current project
     */
    public MagentoMcpClient(@NotNull final Project project) {
        this.project = project;
    }

    /**
     * Start the MCP client.
     *
     * @return True if client started successfully, false otherwise
     */
    public boolean start() {
        if (isRunning) {
            LOGGER.info(SERVER_NAME + " is already running on port " + port);
            return true;
        }

        try {
            client = HttpServer.create(new InetSocketAddress(port), DEFAULT_BACKLOG);
            
            // Register handlers for different endpoints
            client.createContext("/mcp/module/create", new ModuleCreationHandler(project));
            client.createContext("/mcp/entity/create", new EntityCreationHandler(project));
            client.createContext("/mcp/datamodel/create", new DataModelCreationHandler(project));
            
            // Set executor for handling requests
            client.setExecutor(Executors.newCachedThreadPool());
            
            // Start the server
            client.start();
            isRunning = true;
            
            LOGGER.info(SERVER_NAME + " started on port " + port);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to start " + SERVER_NAME + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Stop the MCP client.
     */
    public void stop() {
        if (!isRunning || client == null) {
            return;
        }

        client.stop(0); // Stop immediately
        isRunning = false;
        LOGGER.info(SERVER_NAME + " stopped");
    }

    /**
     * Set the port for the client.
     * Must be called before starting the client.
     *
     * @param port Port number
     */
    public void setPort(final int port) {
        if (isRunning) {
            LOGGER.warn("Cannot change port while client is running");
            return;
        }
        this.port = port;
    }

    /**
     * Check if the client is running.
     *
     * @return True if client is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get the port the client is running on.
     *
     * @return Port number
     */
    public int getPort() {
        return port;
    }
}