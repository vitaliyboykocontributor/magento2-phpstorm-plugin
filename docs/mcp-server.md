# Magento MCP Server

The Magento MCP (Model Context Protocol) Server is a component of the Magento PhpStorm Plugin that allows AI agents and other tools to interact with the Magento code generation capabilities programmatically.

## Overview

The MCP server provides HTTP endpoints that can be used to generate Magento modules and other code artifacts. It uses the same code generation logic as the PhpStorm plugin UI, but exposes it through a REST API.

## Server Configuration

The MCP server is automatically started when the PhpStorm plugin is loaded. By default, it runs on port 8090. You can change the port by modifying the `DEFAULT_PORT` constant in the `MagentoMcpServer` class.

## Endpoints

### Create Module

**Endpoint:** `/mcp/module/create`

**Method:** POST

**Request Body:**

```json
{
  "packageName": "Vendor",
  "moduleName": "Module",
  "moduleDescription": "Description of the module",
  "moduleVersion": "1.0.0",
  "licenses": ["OSL-3.0", "AFL-3.0"],
  "dependencies": ["magento/framework"],
  "createReadme": true
}
```

**Parameters:**

- `packageName` (required): The vendor/package name (e.g., "Vendor")
- `moduleName` (required): The module name (e.g., "Module")
- `moduleDescription` (required): A description of the module
- `moduleVersion` (required): The module version (e.g., "1.0.0")
- `licenses` (optional): An array of license identifiers (e.g., ["OSL-3.0", "AFL-3.0"])
- `dependencies` (optional): An array of module dependencies (e.g., ["magento/framework"])
- `createReadme` (optional): Whether to create a README.md file (default: false)

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

**Error Response:**

```json
{
  "success": false,
  "message": "Error message",
  "generatedFiles": []
}
```

## Usage Examples

### Create a Module

```bash
curl -X POST http://localhost:8090/mcp/module/create \
  -H "Content-Type: application/json" \
  -d '{
    "packageName": "Vendor",
    "moduleName": "Module",
    "moduleDescription": "Description of the module",
    "moduleVersion": "1.0.0",
    "licenses": ["OSL-3.0", "AFL-3.0"],
    "dependencies": ["magento/framework"],
    "createReadme": true
  }'
```

## Integration with AI Agents

AI agents can use the MCP server to generate Magento code by making HTTP requests to the server endpoints. This allows AI agents to leverage the code generation capabilities of the Magento PhpStorm Plugin without requiring direct access to the PhpStorm UI.

### Example: Creating a Module with an AI Agent

1. The AI agent receives a request to create a new Magento module
2. The agent determines the module name, description, and other parameters
3. The agent makes an HTTP request to the MCP server to create the module
4. The MCP server generates the module files and returns the result
5. The agent processes the result and provides feedback to the user

## Troubleshooting

### Server Not Starting

If the MCP server fails to start, check the PhpStorm log for error messages. Common issues include:

- Port 8090 is already in use by another application
- The Magento path is not set correctly in the PhpStorm plugin settings

### Request Errors

If you receive an error response from the server, check the error message for details. Common issues include:

- Missing required parameters in the request
- Invalid parameter values
- The Magento path is not set correctly in the PhpStorm plugin settings
- The base directory for the vendor could not be found

## Future Enhancements

Future versions of the MCP server may include additional endpoints for other code generation capabilities, such as:

- Creating controllers
- Creating blocks
- Creating models
- Creating observers
- Creating plugins
- Creating UI components