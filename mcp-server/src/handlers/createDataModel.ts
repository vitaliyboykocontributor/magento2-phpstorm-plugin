import fetch from 'node-fetch';

// Define the interface for data model property data
interface DataModelProperty {
  name: string;
  type: string;  // Valid types: "int", "float", "string", "bool", "array"
}

// Define the interface for data model creation request parameters
interface DataModelCreationParams {
  // Required fields
  moduleName: string;
  modelName: string;
  
  // Optional fields
  createInterface?: boolean;     // Default: false
  
  // Properties (custom fields) - supports both formats:
  // 1. Array format: [{ name: "field_name", type: "string" }, ...]
  // 2. String format: "field_name:string,other_field:int,..."
  properties?: DataModelProperty[] | string;
}

// Define the interface for data model creation response
interface DataModelCreationResponse {
  success: boolean;
  message: string;
  generatedFiles: string[];
}

/**
 * Parse properties from string format to array format
 * Converts "name:type,name:type" to [{ name: "name", type: "type" }, ...]
 * 
 * @param properties Properties in string or array format
 * @returns Properties in array format
 */
function parseProperties(properties?: DataModelProperty[] | string): DataModelProperty[] {
  if (!properties) {
    return [];
  }
  
  // If already array format, return as-is
  if (Array.isArray(properties)) {
    return properties;
  }
  
  // Parse string format: "name:type,name:type,..."
  if (typeof properties === 'string') {
    const propertyPairs = properties.split(',').map(pair => pair.trim()).filter(pair => pair.length > 0);
    const parsedProperties: DataModelProperty[] = [];
    
    for (const pair of propertyPairs) {
      const [name, type] = pair.split(':').map(part => part.trim());
      if (name && type) {
        // Validate type is supported
        const validTypes = ['int', 'float', 'string', 'bool', 'array'];
        if (!validTypes.includes(type)) {
          throw new Error(`Invalid property type "${type}". Valid types are: ${validTypes.join(', ')}`);
        }
        parsedProperties.push({ name, type });
      } else {
        throw new Error(`Invalid property format "${pair}". Expected format: "name:type"`);
      }
    }
    
    return parsedProperties;
  }
  
  return [];
}

/**
 * Validate property names follow lower_snake_case pattern
 * Similar to NewDataModelDialog validation logic
 * 
 * @param properties Properties array to validate
 */
function validatePropertyNames(properties: DataModelProperty[]): void {
  const lowerSnakeCasePattern = /^[a-z][a-z0-9]*(_[a-z0-9]+)*$/;
  
  for (const property of properties) {
    if (!property.name) {
      throw new Error('Property name cannot be empty');
    }
    
    if (!lowerSnakeCasePattern.test(property.name)) {
      throw new Error(`Property name "${property.name}" must be in lower_snake_case format`);
    }
  }
}

/**
 * Handler for data model creation requests
 * 
 * @param params Data model creation parameters
 * @returns Response with success status, message, and generated files
 */
export async function createDataModelHandler(params: DataModelCreationParams): Promise<DataModelCreationResponse> {
  // Validate required parameters
  if (!params.moduleName) {
    throw new Error('Module name is required');
  }
  if (!params.modelName) {
    throw new Error('Model name is required');
  }

  // Parse and validate properties
  const parsedProperties = parseProperties(params.properties);
  
  // Validate that at least one property is provided (similar to NewDataModelDialog validation)
  if (parsedProperties.length === 0) {
    throw new Error('At least one property is required for the data model');
  }
  
  // Validate property names follow lower_snake_case pattern
  validatePropertyNames(parsedProperties);

  // Prepare request body
  const requestBody = {
    moduleName: params.moduleName,
    modelName: params.modelName,
    createInterface: params.createInterface !== undefined ? params.createInterface : false,
    properties: parsedProperties
  };

  // Get MCP client URL from environment variable or use default
  // Using explicit IPv4 address (127.0.0.1) instead of localhost to avoid IPv6 resolution issues
  const mcpClientUrl = process.env.MCP_CLIENT_URL || 'http://127.0.0.1:8090';
  const endpoint = `${mcpClientUrl}/mcp/datamodel/create`;

  try {
    // Make request to Magento MCP client
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to create data model: ${response.status} ${response.statusText} - ${errorText}`);
    }

    // Parse and return response
    const result = await response.json() as DataModelCreationResponse;
    return result;
  } catch (error: unknown) {
    console.error('Error creating data model:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error occurred',
      generatedFiles: [],
    };
  }
}