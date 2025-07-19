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
 * MCP (Model Context Protocol) server for Magento code generation.
 * This server allows AI agents to interact with the Magento code generation capabilities.
 */
@Service(Service.Level.PROJECT)
public final class MagentoMcpServer {
    private static final Logger LOGGER = Logger.getInstance(MagentoMcpServer.class);
    private static final int DEFAULT_PORT = 8090;
    private static final int DEFAULT_BACKLOG = 0;
    private static final String SERVER_NAME = "Magento MCP Server";

    private final Project project;
    private HttpServer server;
    private int port = DEFAULT_PORT;
    private boolean isRunning = false;

    /**
     * Constructor.
     *
     * @param project Current project
     */
    public MagentoMcpServer(@NotNull final Project project) {
        this.project = project;
    }

    /**
     * Start the MCP server.
     *
     * @return True if server started successfully, false otherwise
     */
    public boolean start() {
        if (isRunning) {
            LOGGER.info(SERVER_NAME + " is already running on port " + port);
            return true;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), DEFAULT_BACKLOG);
            
            // Register handlers for different endpoints
            server.createContext("/mcp/module/create", new ModuleCreationHandler(project));
            
            // Set executor for handling requests
            server.setExecutor(Executors.newCachedThreadPool());
            
            // Start the server
            server.start();
            isRunning = true;
            
            LOGGER.info(SERVER_NAME + " started on port " + port);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to start " + SERVER_NAME + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Stop the MCP server.
     */
    public void stop() {
        if (!isRunning || server == null) {
            return;
        }

        server.stop(0); // Stop immediately
        isRunning = false;
        LOGGER.info(SERVER_NAME + " stopped");
    }

    /**
     * Set the port for the server.
     * Must be called before starting the server.
     *
     * @param port Port number
     */
    public void setPort(final int port) {
        if (isRunning) {
            LOGGER.warn("Cannot change port while server is running");
            return;
        }
        this.port = port;
    }

    /**
     * Check if the server is running.
     *
     * @return True if server is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get the port the server is running on.
     *
     * @return Port number
     */
    public int getPort() {
        return port;
    }
}