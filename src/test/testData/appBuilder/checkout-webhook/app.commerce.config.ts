import { defineConfig } from "@adobe/aio-commerce-lib-app/config";

export default defineConfig({
  metadata: { id: "fixture-stock-validation", displayName: "Stock validation", version: "1.0.0", description: "Synchronous checkout fixture" },
  webhooks: [{
    category: "validation", label: "Check stock", description: "Reject unavailable cart items",
    requireAdobeAuth: true,
    runtimeAction: "webhook/check-stock",
    webhook: {
      webhook_method: "observer.checkout_cart_product_add_before", webhook_type: "before",
      batch_name: "validate_stock", hook_name: "check_stock", method: "POST", required: true,
      soft_timeout: 1000, timeout: 5000, fallback_error_message: "Stock validation unavailable",
      fields: [{ name: "data.cart_id" }, { name: "data.items" }],
    },
  }],
});
