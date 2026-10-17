const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const catalog = require('./catalog.json');
const read = file => JSON.parse(fs.readFileSync(path.join(__dirname, file), 'utf8'));
const action = file => require(path.join(__dirname, file)).main;

test('catalog targets and application roots exist; expected source references remain present', () => {
  for (const fixture of catalog) {
    const root = path.join(__dirname, fixture.name);
    assert.equal(read(`${fixture.name}/package.json`).private, true);
    for (const appRoot of fixture.roots) assert.ok(fs.statSync(path.join(root, appRoot)).isDirectory());
    for (const ref of [...fixture.navigation, ...(fixture.missing || [])]) {
      assert.ok(fs.readFileSync(path.join(root, ref.source), 'utf8').includes(ref.reference), `${fixture.name}: ${ref.reference}`);
      if (ref.target) assert.ok(fs.statSync(path.join(root, ref.target)).isFile());
    }
  }
});

test('standalone catalog consumer accepts a product and rejects incomplete input', async () => {
  const main = action('standalone-events/actions/catalog/updated.js');
  assert.deepEqual(await main(read('standalone-events/payloads/product-updated.json')), {
    statusCode: 200, body: { sku: 'TEST-SKU', operation: 'upsert' },
  });
  assert.equal((await main({})).statusCode, 400);
  assert.equal((await action('standalone-events/actions/health/index.js')({})).body.healthy, true);
});

test('same-named order handlers have distinct directions and audit accepts the event', async () => {
  const payload = read('commerce-integration/payloads/order-updated.json');
  for (const [flow, direction] of [['commerce', 'to-erp'], ['external', 'to-commerce']]) {
    const main = action(`commerce-integration/src/commerce-extensibility-1/actions/order/${flow}/updated/index.js`);
    assert.deepEqual((await main(payload)).body, { order: 'TEST-1001', direction });
    assert.equal((await main({})).statusCode, 400);
  }
  const audit = action('commerce-integration/src/commerce-extensibility-1/actions/audit/record.js');
  assert.deepEqual((await audit(payload)).body, { eventId: payload.id, recorded: true });
});

test('checkout returns success or exception operations synchronously', async () => {
  const main = action('checkout-webhook/src/commerce-extensibility-1/actions/check-stock/index.js');
  assert.deepEqual(await main(read('checkout-webhook/payloads/in-stock.json')), { statusCode: 200, body: [{ op: 'success' }] });
  assert.deepEqual(await main(read('checkout-webhook/payloads/out-of-stock.json')), {
    statusCode: 200, body: [{ op: 'exception', message: 'Requested stock is unavailable' }],
  });
  assert.deepEqual(await main({}), { statusCode: 200, body: [{ op: 'exception', message: 'Missing items' }] });
});

test('identically named actions in sibling apps keep distinct implementations', async () => {
  for (const app of ['catalog', 'orders']) {
    const main = action(`multi-app-workspace/apps/${app}/actions/updated/index.js`);
    assert.equal((await main({})).body.application, app);
  }
});
