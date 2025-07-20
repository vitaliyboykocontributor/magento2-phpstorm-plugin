#!/bin/bash

# Run script for Magento MCP Server

# Default values
MCP_CLIENT_URL=${MCP_CLIENT_URL:-"http://host.docker.internal:8090"}
PORT=${PORT:-3000}
LOG_LEVEL=${LOG_LEVEL:-"info"}

# Help message
show_help() {
  echo "Usage: ./run.sh [options]"
  echo ""
  echo "Options:"
  echo "  --help                 Show this help message"
  echo "  --mcp-client-url URL   Set the MCP client URL (default: $MCP_CLIENT_URL)"
  echo "  --port PORT            Set the server port (default: $PORT)"
  echo "  --log-level LEVEL      Set the log level (debug, info, warn, error) (default: $LOG_LEVEL)"
  echo ""
  echo "Example:"
  echo "  ./run.sh --mcp-client-url http://localhost:8090 --port 3000 --log-level debug"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --help)
      show_help
      exit 0
      ;;
    --mcp-client-url)
      MCP_CLIENT_URL="$2"
      shift 2
      ;;
    --port)
      PORT="$2"
      shift 2
      ;;
    --log-level)
      LOG_LEVEL="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      show_help
      exit 1
      ;;
  esac
done

echo "Running Magento MCP Server..."
echo "MCP Client URL: $MCP_CLIENT_URL"
echo "Port: $PORT"
echo "Log Level: $LOG_LEVEL"

# Run the Docker container
docker run -i --rm \
  -p $PORT:3000 \
  -e MCP_CLIENT_URL=$MCP_CLIENT_URL \
  -e PORT=3000 \
  -e LOG_LEVEL=$LOG_LEVEL \
  mcp/magento-server

# Note: The -i flag is required for MCP to work properly
# The --rm flag removes the container when it exits