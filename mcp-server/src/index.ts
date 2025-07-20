/**
 * Magento MCP Server - Main Entry Point
 * 
 * This implementation uses the official MCP TypeScript SDK to create an MCP-compliant server.
 * We import the McpServer class from the server/mcp.js module and the StdioServerTransport
 * from the server/stdio.js module as defined in the SDK's package.json exports.
 * 
 * IMPORTANT: When using StdioServerTransport, all communication with the IDE happens over
 * stdout using the JSON-RPC protocol. To prevent log messages from interfering with this
 * communication and causing JSON parsing errors in the IDE, this server redirects all logs
 * to stderr instead of stdout.
 * 
 * This approach ensures that:
 * 1. The IDE receives only valid JSON-RPC messages on stdout
 * 2. Log messages are still visible in the Docker logs
 * 3. No "Unexpected JSON token" errors occur when adding the server to the IDE
 */
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import dotenv from 'dotenv';
import { z } from 'zod';
import { createModuleHandler } from './handlers/createModule';

// Load environment variables from .env file
dotenv.config();

// Configure logging - redirect all logs to stderr to avoid interfering with JSON-RPC communication on stdout
const logLevel = process.env.LOG_LEVEL || 'info';
const logger = {
  debug: (message: string, ...args: any[]) => {
    if (logLevel === 'debug') console.error(`[DEBUG] ${message}`, ...args);
  },
  info: (message: string, ...args: any[]) => {
    if (['debug', 'info'].includes(logLevel)) console.error(`[INFO] ${message}`, ...args);
  },
  warn: (message: string, ...args: any[]) => {
    if (['debug', 'info', 'warn'].includes(logLevel)) console.error(`[WARN] ${message}`, ...args);
  },
  error: (message: string, ...args: any[]) => {
    console.error(`[ERROR] ${message}`, ...args);
  }
};

// Log SDK initialization
logger.info('Initializing MCP server from SDK...');

// Create MCP server instance with required parameters
const server = new McpServer({
  name: 'magento-mcp-server',
  version: '1.0.0',
});

// Register the magento_create_module tool
server.registerTool(
  'magento_create_module',
  {
    title: 'Create Magento Module',
    description: 'Creates a new Magento module with the specified parameters',
    inputSchema: {
      packageName: z.string().describe('The vendor/package name (e.g., "Vendor")'),
      moduleName: z.string().describe('The module name (e.g., "Module")'),
      moduleDescription: z.string().describe('A description of the module'),
      moduleVersion: z.string().describe('The module version (e.g., "1.0.0")'),
      licenses: z.array(z.string()).optional().describe('An array of license identifiers (e.g., ["OSL-3.0", "AFL-3.0"])'),
      dependencies: z.array(z.string()).optional().describe('An array of module dependencies (e.g., ["magento/framework"])'),
      createReadme: z.boolean().optional().describe('Whether to create a README.md file')
    }
  },
  async (params: any) => {
    logger.info('Handling magento_create_module request with params:', params);
    try {
      // Call the createModuleHandler with the provided parameters
      const result = await createModuleHandler(params);
      logger.info('Module creation completed:', result);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result)
          }
        ]
      };
    } catch (error: unknown) {
      logger.error('Error in magento_create_module handler:', error);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify({
              success: false,
              message: error instanceof Error ? error.message : 'Unknown error occurred',
              generatedFiles: []
            })
          }
        ],
        isError: true
      };
    }
  }
);

// Log server configuration
logger.info('Starting Magento MCP Server with configuration:', {
  mcpClientUrl: process.env.MCP_CLIENT_URL || 'http://localhost:8090',
  port: process.env.PORT || '3000',
  logLevel
});

// Start the server
async function init() {
  try {
    // Create a stdio transport for the server
    const transport = new StdioServerTransport();
    
    // Connect the server to the transport
    await server.connect(transport);
    
    logger.info('Magento MCP Server started');
    
    // Handle graceful shutdown
    process.on('SIGINT', () => {
      logger.info('Shutting down Magento MCP Server...');
      process.exit(0);
    });
  } catch (error: unknown) {
    logger.error('Failed to start MCP server:', error);
    process.exit(1);
  }
}

// Initialize the server
init();