exports.main = async function main(params) {
  const items = params.data?.items;
  if (!Array.isArray(items)) return { statusCode: 200, body: [{ op: "exception", message: "Missing items" }] };
  const unavailable = items.some(item => item.qty > 5);
  return { statusCode: 200, body: unavailable
    ? [{ op: "exception", message: "Requested stock is unavailable" }]
    : [{ op: "success" }] };
};
