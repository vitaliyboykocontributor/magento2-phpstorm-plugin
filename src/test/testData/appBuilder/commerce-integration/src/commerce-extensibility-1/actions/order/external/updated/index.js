exports.main = async function main(params) {
  const order = params.data?.value;
  if (!order?.increment_id) return { statusCode: 400, body: { error: "Missing order number" } };
  return { statusCode: 200, body: { order: order.increment_id, direction: "to-commerce" } };
};
