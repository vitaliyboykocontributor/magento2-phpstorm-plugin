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
import { createEntityHandler } from './handlers/createEntity';

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

// Register the magento_create_entity tool
server.registerTool(
  'magento_create_entity',
  {
    title: 'Create Magento Entity',
    description: 'Creates a new Magento entity with model, resource model, collection, controllers, and admin UI components',
    inputSchema: {
      moduleName: z.string().describe('The module name in Vendor_ModuleName format (e.g., "Vendor_Module")'),
      entityName: z.string().describe('The entity name (e.g., "Product")'),
      tableName: z.string().optional().describe('The database table name (auto-generated if not provided)'),
      idFieldName: z.string().optional().describe('The ID field name (auto-generated if not provided)'),
      tableEngine: z.string().optional().describe('Database table engine (default: "innodb")'),
      tableResource: z.string().optional().describe('Database table resource (default: "default")'),
      hasAdminUiComponents: z.boolean().optional().describe('Whether to generate admin UI components (default: true)'),
      hasDtoInterface: z.boolean().optional().describe('Whether to generate DTO interface (default: true)'),
      hasWebApi: z.boolean().optional().describe('Whether to generate Web API (default: false)'),
      route: z.string().optional().describe('Admin route for the entity (auto-generated if not provided)'),
      formLabel: z.string().optional().describe('Admin form label (auto-generated if not provided)'),
      formName: z.string().optional().describe('Admin form name (auto-generated if not provided)'),
      gridName: z.string().optional().describe('Admin grid name (auto-generated if not provided)'),
      hasToolbar: z.boolean().optional().describe('Whether to generate toolbar (default: true)'),
      hasToolbarBookmarks: z.boolean().optional().describe('Whether to generate toolbar bookmarks (default: true)'),
      hasToolbarColumnsControl: z.boolean().optional().describe('Whether to generate columns control (default: true)'),
      hasToolbarListingFilters: z.boolean().optional().describe('Whether to generate listing filters (default: true)'),
      hasToolbarListingPaging: z.boolean().optional().describe('Whether to generate listing paging (default: true)'),
      parentAclId: z.string().optional().describe('Parent ACL resource ID (optional)'),
      aclId: z.string().optional().describe('ACL resource ID (auto-generated if not provided)'),
      aclTitle: z.string().optional().describe('ACL resource title (auto-generated if not provided)'),
      parentMenuId: z.string().optional().describe('Parent menu ID (optional)'),
      menuSortOrder: z.number().optional().describe('Menu sort order (default: 100)'),
      menuId: z.string().optional().describe('Menu ID (auto-generated if not provided)'),
      menuTitle: z.string().optional().describe('Menu title (auto-generated if not provided)'),
      properties: z.union([
        z.string().describe('Entity properties in string format: "name:type,name:type,..." (e.g., "ip_address:string,port:int,is_active:bool")'),
        z.array(z.object({
          name: z.string().describe('Property name'),
          type: z.string().describe('Property type - valid types: "int", "float", "string", "bool", "array"')
        }))
      ]).optional().describe('Additional entity properties/fields. Supports two formats:\n1. String format: "field_name:type,another_field:type" (e.g., "ip_address:string,number_of_calls:int,route:string")\n2. Array format: [{"name": "field_name", "type": "string"}, {"name": "another_field", "type": "int"}]\nValid types: "int", "float", "string", "bool", "array"')
    }
  },
  async (params: any) => {
    logger.info('Handling magento_create_entity request with params:', params);
    try {
      // Call the createEntityHandler with the provided parameters
      const result = await createEntityHandler(params);
      logger.info('Entity creation completed:', result);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result)
          }
        ]
      };
    } catch (error: unknown) {
      logger.error('Error in magento_create_entity handler:', error);
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
  // Using explicit IPv4 address (127.0.0.1) instead of localhost to avoid IPv6 resolution issues
  mcpClientUrl: process.env.MCP_CLIENT_URL || 'http://127.0.0.1:8090',
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