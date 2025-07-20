// Test script to verify SDK import
console.log('Testing SDK import...');

try {
  // Try CommonJS import
  console.log('Trying CommonJS import...');
  const sdk = require('@modelcontextprotocol/sdk');
  console.log('CommonJS import successful!');
  console.log('SDK structure:', Object.keys(sdk));
} catch (error) {
  console.error('CommonJS import failed:', error.message);
}

// Exit after tests
console.log('Import tests completed');