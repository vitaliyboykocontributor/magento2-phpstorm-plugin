import fetch from 'node-fetch';

// Define the interface for entity property data
interface EntityProperty {
  name: string;
  type: string;  // int, varchar, text, datetime, etc.
}

// Define the interface for entity creation request parameters
interface EntityCreationParams {
  // Required basic fields
  moduleName: string;
  entityName: string;
  tableName: string;
  idFieldName: string;
  
  // Optional database settings
  tableEngine?: string;         // Default: "innodb"
  tableResource?: string;       // Default: "default"
  
  // Feature flags
  hasAdminUiComponents?: boolean; // Default: true
  hasDtoInterface?: boolean;      // Default: true
  hasWebApi?: boolean;           // Default: false
  
  // UI Component settings (if hasAdminUiComponents = true)
  route?: string;
  formLabel?: string;
  formName?: string;
  gridName?: string;
  hasToolbar?: boolean;
  hasToolbarBookmarks?: boolean;
  hasToolbarColumnsControl?: boolean;
  hasToolbarListingFilters?: boolean;
  hasToolbarListingPaging?: boolean;
  
  // ACL settings
  parentAclId?: string;
  aclId?: string;
  aclTitle?: string;
  
  // Menu settings
  parentMenuId?: string;
  menuSortOrder?: number;       // Default: 100
  menuId?: string;
  menuTitle?: string;
  
  // Entity properties (custom fields)
  properties?: EntityProperty[];
}

// Define the interface for entity creation response
interface EntityCreationResponse {
  success: boolean;
  message: string;
  generatedFiles: string[];
}

/**
 * Auto-complete entity identifiers based on entity name
 * Similar to NewEntityDialog.autoCompleteIdentifiers()
 */
function autoCompleteIdentifiers(params: EntityCreationParams): EntityCreationParams {
  const entityName = params.entityName;
  if (!entityName) {
    return params;
  }

  // Convert camelCase to snake_case for table name
  const snakeCase = entityName
    .replace(/([A-Z])/g, '_$1')
    .toLowerCase()
    .replace(/^_/, '');
  
  // Create entity name label (Title Case with spaces)
  const entityNameLabel = snakeCase
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  // Auto-complete missing fields
  const completed = { ...params };
  
  if (!completed.tableName) {
    completed.tableName = snakeCase;
  }
  
  if (!completed.idFieldName) {
    completed.idFieldName = `${snakeCase}_id`;
  }
  
  if (!completed.route) {
    completed.route = snakeCase;
  }
  
  if (!completed.formLabel) {
    completed.formLabel = `${entityNameLabel} Form`;
  }
  
  if (!completed.formName) {
    completed.formName = `${snakeCase}_form`;
  }
  
  if (!completed.gridName) {
    completed.gridName = `${snakeCase}_listing`;
  }
  
  if (!completed.aclId) {
    completed.aclId = `${params.moduleName}::management`;
  }
  
  if (!completed.aclTitle) {
    completed.aclTitle = `${entityNameLabel} Management`;
  }
  
  if (!completed.menuId) {
    completed.menuId = `${params.moduleName}::management`;
  }
  
  if (!completed.menuTitle) {
    completed.menuTitle = `${entityNameLabel} Management`;
  }

  return completed;
}

/**
 * Handler for entity creation requests
 * 
 * @param params Entity creation parameters
 * @returns Response with success status, message, and generated files
 */
export async function createEntityHandler(params: EntityCreationParams): Promise<EntityCreationResponse> {
  // Validate required parameters
  if (!params.moduleName) {
    throw new Error('Module name is required');
  }
  if (!params.entityName) {
    throw new Error('Entity name is required');
  }
  if (!params.tableName) {
    throw new Error('Table name is required');
  }
  if (!params.idFieldName) {
    throw new Error('ID field name is required');
  }

  // Auto-complete identifiers
  const completedParams = autoCompleteIdentifiers(params);

  // Apply default values
  const requestBody = {
    moduleName: completedParams.moduleName,
    entityName: completedParams.entityName,
    tableName: completedParams.tableName,
    idFieldName: completedParams.idFieldName,
    tableEngine: completedParams.tableEngine || 'innodb',
    tableResource: completedParams.tableResource || 'default',
    hasAdminUiComponents: completedParams.hasAdminUiComponents !== undefined ? completedParams.hasAdminUiComponents : true,
    hasDtoInterface: completedParams.hasDtoInterface !== undefined ? completedParams.hasDtoInterface : true,
    hasWebApi: completedParams.hasWebApi !== undefined ? completedParams.hasWebApi : false,
    route: completedParams.route,
    formLabel: completedParams.formLabel,
    formName: completedParams.formName,
    gridName: completedParams.gridName,
    hasToolbar: completedParams.hasToolbar !== undefined ? completedParams.hasToolbar : true,
    hasToolbarBookmarks: completedParams.hasToolbarBookmarks !== undefined ? completedParams.hasToolbarBookmarks : true,
    hasToolbarColumnsControl: completedParams.hasToolbarColumnsControl !== undefined ? completedParams.hasToolbarColumnsControl : true,
    hasToolbarListingFilters: completedParams.hasToolbarListingFilters !== undefined ? completedParams.hasToolbarListingFilters : true,
    hasToolbarListingPaging: completedParams.hasToolbarListingPaging !== undefined ? completedParams.hasToolbarListingPaging : true,
    parentAclId: completedParams.parentAclId || '',
    aclId: completedParams.aclId,
    aclTitle: completedParams.aclTitle,
    parentMenuId: completedParams.parentMenuId || '',
    menuSortOrder: completedParams.menuSortOrder || 100,
    menuId: completedParams.menuId,
    menuTitle: completedParams.menuTitle,
    properties: completedParams.properties || []
  };

  // Get MCP client URL from environment variable or use default
  // Using explicit IPv4 address (127.0.0.1) instead of localhost to avoid IPv6 resolution issues
  const mcpClientUrl = process.env.MCP_CLIENT_URL || 'http://127.0.0.1:8090';
  const endpoint = `${mcpClientUrl}/mcp/entity/create`;

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
      throw new Error(`Failed to create entity: ${response.status} ${response.statusText} - ${errorText}`);
    }

    // Parse and return response
    const result = await response.json() as EntityCreationResponse;
    return result;
  } catch (error: unknown) {
    console.error('Error creating entity:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error occurred',
      generatedFiles: [],
    };
  }
}