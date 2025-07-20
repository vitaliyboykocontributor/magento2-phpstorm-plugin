import fetch from 'node-fetch';

// Define the interface for module creation request parameters
interface ModuleCreationParams {
  packageName: string;
  moduleName: string;
  moduleDescription: string;
  moduleVersion: string;
  licenses?: string[];
  dependencies?: string[];
  createReadme?: boolean;
}

// Define the interface for module creation response
interface ModuleCreationResponse {
  success: boolean;
  message: string;
  generatedFiles: string[];
}

/**
 * Handler for module creation requests
 * 
 * @param params Module creation parameters
 * @returns Response with success status, message, and generated files
 */
export async function createModuleHandler(params: ModuleCreationParams): Promise<ModuleCreationResponse> {
  // Validate required parameters
  if (!params.packageName) {
    throw new Error('Package name is required');
  }
  if (!params.moduleName) {
    throw new Error('Module name is required');
  }
  if (!params.moduleDescription) {
    throw new Error('Module description is required');
  }
  if (!params.moduleVersion) {
    throw new Error('Module version is required');
  }

  // Get MCP client URL from environment variable or use default
  const mcpClientUrl = process.env.MCP_CLIENT_URL || 'http://localhost:8090';
  const endpoint = `${mcpClientUrl}/mcp/module/create`;

  try {
    // Make request to Magento MCP client
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        packageName: params.packageName,
        moduleName: params.moduleName,
        moduleDescription: params.moduleDescription,
        moduleVersion: params.moduleVersion,
        licenses: params.licenses || ['OSL-3.0', 'AFL-3.0'],
        dependencies: params.dependencies || ['magento/framework'],
        createReadme: params.createReadme !== undefined ? params.createReadme : true,
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to create module: ${response.status} ${response.statusText} - ${errorText}`);
    }

    // Parse and return response
    const result = await response.json() as ModuleCreationResponse;
    return result;
  } catch (error: unknown) {
    console.error('Error creating module:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error occurred',
      generatedFiles: [],
    };
  }
}