exports.main = async function main(params) {
  const product = params.data?.value;
  if (!product?.sku) return { statusCode: 400, body: { error: "Missing product SKU" } };
  return { statusCode: 200, body: { sku: product.sku, operation: "upsert" } };
};
