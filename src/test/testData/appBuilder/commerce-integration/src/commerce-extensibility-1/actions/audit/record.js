exports.main = async function main(params) {
  return { statusCode: 200, body: { eventId: params.id, recorded: true } };
};
