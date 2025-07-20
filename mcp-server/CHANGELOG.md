# Changelog

## 2025-07-20

### Fixed

- Fixed JSON-RPC error: `JSONRPCError(code=MethodNotFound, message=Method not found, data={})` by switching from `Server` to `McpServer` class and properly implementing tool registration

- Fixed JSON parsing error: `Unexpected JSON token at offset 21: Expected end of the array or comma at path: $ JSON input: [INFO] Initializing MCP server from SDK...` by redirecting all logs to stderr instead of stdout
- Fixed TypeScript compilation error: `Expected 2 arguments, but got 1` by updating the server implementation to use the correct SDK imports and method calls
- Fixed module resolution error by updating tsconfig.json to use Node16 module resolution
- Fixed import path error by using the correct paths as defined in the SDK's package.json exports
- Updated documentation to reflect the current implementation and provide guidance for future developers

### Technical Details

#### Original Error

```
src/index.ts(93,8): error TS2554: Expected 2 arguments, but got 1.
```

This error occurred because the `setRequestHandler` method expected a schema parameter that wasn't provided in our implementation.

#### Solution

1. **Updated Import Paths**: Changed from using specific subpaths to using the main SDK entry point:
   ```typescript
   // Before
   import { Server } from '@modelcontextprotocol/sdk/server/index.js';
   
   // After
   import { Server } from '@modelcontextprotocol/sdk';
   ```

2. **Updated TypeScript Configuration**: Modified tsconfig.json to use Node16 module resolution:
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

   Note: The actual tsconfig.json contains additional options for strict type checking and other features.

3. **Simplified Server Implementation**: Focused on the core functionality without trying to use advanced features of the SDK that were causing issues.

4. **Updated Documentation**: Updated README.md with accurate information about the SDK usage and TypeScript configuration.

These changes ensure that the MCP server compiles correctly and runs without errors, providing a stable foundation for future development.

#### JSON Parsing Error Fix

The server was encountering the following error when being added to the IDE:

```
Unexpected JSON token at offset 21: Expected end of the array or comma at path: $ JSON input: [INFO] Initializing MCP server from SDK...

write /dev/stdout: broken pipe
```

**Root Cause**: 
When using StdioServerTransport, all communication with the IDE happens over stdout using the JSON-RPC protocol. The server was logging messages to stdout (using console.log, console.info, etc.), which interfered with this communication. The IDE was trying to parse these log messages as JSON, resulting in parsing errors.

**Solution**:
1. **Redirected All Logs to stderr**: Modified the logger to use console.error for all log levels (debug, info, warn, error):
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

2. **Added Documentation**: Updated README.md with a new "Logging Strategy" section and added detailed comments to the code explaining the approach.

This ensures that stdout is reserved exclusively for JSON-RPC communication, preventing JSON parsing errors in the IDE while still maintaining visibility of logs in the Docker container.