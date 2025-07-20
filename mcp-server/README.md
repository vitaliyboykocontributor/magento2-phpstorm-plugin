# Magento MCP Server

This is a Model Context Protocol (MCP) server implementation for Magento code generation. It allows AI agents to interact with the Magento code generation capabilities through the MCP protocol.

## Features

- MCP-compliant server implementation using the official SDK
- Module creation functionality
- Docker support for easy deployment
- Configurable through environment variables

## Prerequisites

- Node.js 18 or later (for local development)
- Docker (for containerized deployment)
- Magento PhpStorm Plugin with MCP client enabled

## Installation

### Local Development

1. Clone the repository
2. Navigate to the `mcp-server` directory
3. Install dependencies:

```bash
npm install
```

4. Create a `.env` file based on `.env.example`:

```bash
cp .env.example .env
```

5. Edit the `.env` file to configure the server

### Docker Deployment

1. Build the Docker image:

```bash
docker build -t mcp/magento-server .
```

2. Run the Docker container:

```bash
docker run -p 3000:3000 -e MCP_CLIENT_URL=http://host.docker.internal:8090 mcp/magento-server
```

## Configuration

The server can be configured using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| MCP_CLIENT_URL | URL of the Magento MCP client | http://localhost:8090 |
| PORT | Port for the MCP server to listen on | 3000 |
| LOG_LEVEL | Logging level (debug, info, warn, error) | info |

## Usage

### MCP Functions

The server provides the following MCP functions:

#### magento_create_module

Creates a new Magento module.

**Parameters:**

- `packageName` (required): The vendor/package name (e.g., "Vendor")
- `moduleName` (required): The module name (e.g., "Module")
- `moduleDescription` (required): A description of the module
- `moduleVersion` (required): The module version (e.g., "1.0.0")
- `licenses` (optional): An array of license identifiers (e.g., ["OSL-3.0", "AFL-3.0"])
- `dependencies` (optional): An array of module dependencies (e.g., ["magento/framework"])
- `createReadme` (optional): Whether to create a README.md file (default: true)

**Response:**

```json
{
  "success": true,
  "message": "Module generated successfully",
  "generatedFiles": [
    "/path/to/app/code/Vendor/Module/composer.json",
    "/path/to/app/code/Vendor/Module/registration.php",
    "/path/to/app/code/Vendor/Module/etc/module.xml",
    "/path/to/app/code/Vendor/Module/README.md"
  ]
}
```

## Integration with JetBrains IDE

To use this MCP server with JetBrains IDE, add the following configuration to your IDE settings:

```json
{
  "mcpServers": {
    "magento-server": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "MCP_CLIENT_URL=http://host.docker.internal:8090",
        "mcp/magento-server"
      ],
      "env": {}
    }
  }
}
```

## Development

### Building the TypeScript Code

```bash
npm run build
```

### Running in Development Mode

```bash
npm run dev
```

### TypeScript SDK Information

This project uses the Model Context Protocol TypeScript SDK version 1.16.0. We import the SDK components as follows:

```typescript
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';
```

This approach follows the SDK's package.json exports configuration, ensuring we're using the official SDK components correctly. The server implementation:

1. Creates an instance of McpServer with appropriate name and version
2. Registers tools using the registerTool method with Zod schemas for input validation
3. Sets up a StdioServerTransport for communication
4. Connects the server to the transport

We use Zod (a TypeScript-first schema validation library) for defining the input schemas of our tools, which provides strong type checking and validation for the parameters passed to our tools.

### Logging Strategy

When using StdioServerTransport, all communication with the IDE happens over stdout using the JSON-RPC protocol. To prevent log messages from interfering with this communication and causing JSON parsing errors, this server redirects all logs to stderr instead of stdout:

```typescript
// Configure logging - redirect all logs to stderr to avoid interfering with JSON-RPC communication on stdout
const logger = {
  debug: (message, ...args) => {
    if (logLevel === 'debug') console.error(`[DEBUG] ${message}`, ...args);
  },
  info: (message, ...args) => {
    if (['debug', 'info'].includes(logLevel)) console.error(`[INFO] ${message}`, ...args);
  },
  // ... other log levels
};
```

This approach ensures that:
1. The IDE receives only valid JSON-RPC messages on stdout
2. Log messages are still visible in the Docker logs
3. No "Unexpected JSON token" errors occur when adding the server to the IDE

When running the server with Docker, both stdout and stderr are captured in the container logs, so you'll still see all log messages in the terminal.

### Handler Registration

The handler for the `magento_create_module` tool is registered using the `registerTool` method of the `McpServer` class:

```typescript
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
  async (params) => {
    // Call the createModuleHandler with the provided parameters
    const result = await createModuleHandler(params);
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result)
        }
      ]
    };
  }
);
```

The `registerTool` method takes three arguments:
1. The tool name (a string)
2. The tool metadata (an object with title, description, and inputSchema)
3. The handler function (an async function that processes the parameters and returns a result)

We use Zod to define the input schema, which provides strong type checking and validation for the parameters passed to our tools. When a tool invocation request is received, the server automatically routes it to the appropriate handler based on the tool name. The handler then calls the `createModuleHandler` function and returns the result in the format expected by the MCP protocol.

#### TypeScript Configuration

The project uses the following TypeScript configuration to ensure compatibility with the SDK:

```json
{
  "compilerOptions": {
    "module": "Node16",
    "moduleResolution": "Node16",
    "outDir": "./dist",
    "rootDir": "./src"
  }
}
```

Note: The actual tsconfig.json contains additional options for strict type checking, module resolution, and other features.

This configuration is necessary to handle the SDK's package structure correctly. The `Node16` module resolution strategy allows TypeScript to correctly resolve imports from the SDK's package.json exports.

If you need to modify the server implementation, refer to the comments in `src/index.ts` and the [official MCP TypeScript SDK documentation](https://github.com/modelcontextprotocol/typescript-sdk) for guidance.

## License

OSL-3.0