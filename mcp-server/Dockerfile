FROM node:18-alpine

WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Fix SDK package.json exports field to expose required subpaths
RUN if [ -d "node_modules/@modelcontextprotocol/sdk" ]; then \
    # Create a Node.js script to properly modify the package.json
    echo 'const fs = require("fs"); \
    const path = require("path"); \
    const pkgPath = path.join(process.cwd(), "node_modules/@modelcontextprotocol/sdk/package.json"); \
    const pkg = JSON.parse(fs.readFileSync(pkgPath, "utf8")); \
    pkg.main = "dist/cjs/server/index.js"; \
    pkg.exports = { \
      ".": { \
        "import": "./dist/esm/server/index.js", \
        "require": "./dist/cjs/server/index.js" \
      }, \
      "./server/mcp.js": { \
        "import": "./dist/esm/server/mcp.js", \
        "require": "./dist/cjs/server/mcp.js" \
      }, \
      "./server/stdio.js": { \
        "import": "./dist/esm/server/stdio.js", \
        "require": "./dist/cjs/server/stdio.js" \
      } \
    }; \
    fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2)); \
    console.log("Fixed SDK package.json with required subpaths");' > fix-sdk.js && \
    node fix-sdk.js && \
    rm fix-sdk.js; \
    fi

# Copy source code
COPY . .

# Build TypeScript code
RUN npm run build

# Expose the port the server listens on
EXPOSE 3000

# Set environment variables
ENV NODE_ENV=production
ENV MCP_CLIENT_URL=http://localhost:8090
ENV LOG_LEVEL=info

# Start the server
CMD ["node", "dist/index.js"]