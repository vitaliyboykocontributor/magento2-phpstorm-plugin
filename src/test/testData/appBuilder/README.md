# App Builder project fixtures

Small, authored projects for developing the plugin, not production integrations or full starter-kit copies. Open any scenario folder directly in PhpStorm, WebStorm, or IntelliJ IDEA with JavaScript support. Set **Tools > Adobe App Builder > Application directory** to a root listed below. Use a disposable copy when experimenting so IDE files and edits do not change test inputs.

| Project | Select directory | Scenario and future assertions |
| --- | --- | --- |
| `standalone-events` | project root | JavaScript actions, environment inputs, I/O Events registration → `catalog/updated` → handler; authenticated web action alongside non-web event consumer. |
| `commerce-integration` | project root | TypeScript Commerce config, nested YAML includes, Commerce → ERP and ERP → Commerce flows, event fan-out to audit. Two actions named `updated` must resolve by package. |
| `checkout-webhook` | project root | Synchronous stock validation, `runtimeAction` → manifest → JavaScript handler; successful and rejected cart payloads. |
| `multi-app-workspace` | `apps/catalog` or `apps/orders` | Two npm workspaces with identical package/action names. Selection scopes references to one app; `tools/app.config.yaml` is unrelated. |
| `incomplete-app` | project root | Missing authored handler, stale action reference, missing include, and not-yet-generated output. Expected broken references are listed in `catalog.json`. |
| `plain-node` | none | Negative project-detection control: package.json and app.config.yaml alone do not establish an App Builder project. Explicit directory selection currently accepts any existing directory. |

`catalog.json` is test metadata, not Adobe configuration. It records application roots and expected navigation targets. `AppBuilderNavigationTest` exercises the event/webhook → action declaration → handler links through actual PSI references, including nested includes and incomplete configurations. Each scenario can be copied independently; actions do not import code from other fixtures.

The Commerce fixtures intentionally omit SDK-generated installation/association actions, credentials, cloud provisioning, and real external API calls. They represent the authored integration code, not fully deployable apps. SDK dependencies document the real config import; installation is unnecessary for the local JavaScript smoke tests. TypeScript configs are source fixtures, not type-checked without installing the SDK. Payloads and business rules are synthetic: stock is capped at five units and synchronization returns a proposed operation without making network calls. `.generated` output in `incomplete-app` is intentionally absent, not an ordinary missing authored handler.

## Verification and reuse

Run the offline action/fixture smoke checks from the repository root:

```sh
node --test src/test/testData/appBuilder/fixtures.test.cjs
```

The platform test `AppBuilderProjectFixturesTest` copies projects into temporary directories and checks integration scope against real files, including switching between sibling apps. The WebStorm settings UI test uses `standalone-events` from this same collection. UI fixtures are selected with `appBuilder/<scenario>`; do not duplicate these projects under `src/uiTest/resources`.

When adding a scenario, update `catalog.json`, include representative inputs and expected navigation or errors, and extend the relevant tests. Keep credentials, `.aio`, node_modules, IDE state, and generated build output out of the fixture tree.

## Format references

Reviewed on 2026-10-17; these are reduced examples, not snapshots of the upstream projects:

- [App Builder configuration](https://developer.adobe.com/app-builder/docs/guides/app_builder_guides/configuration/configuration): manifests, inputs and includes. Paths in included YAML resolve relative to the declaring file.
- [I/O Events registrations](https://developer.adobe.com/events/docs/guides/appbuilder/): `events.registrations` and `runtime_action`.
- [Adobe Commerce integration starter kit](https://github.com/adobe/commerce-integration-starter-kit): `app.commerce.config.ts`, `runtimeActions`, `runtimeAction`, nested action configs, and the `@adobe/aio-commerce-lib-app` 2.x dependency.
- [Commerce webhook responses](https://developer.adobe.com/commerce/extensibility/webhooks/responses): synchronous response operations.
