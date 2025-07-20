#!/bin/bash

# Build script for Magento MCP Server

# Exit on error
set -e

echo "Building Magento MCP Server Docker image..."

# Build the Docker image
docker build -t mcp/magento-server .

echo "Docker image built successfully: mcp/magento-server"
echo "You can run the server using: ./run.sh"