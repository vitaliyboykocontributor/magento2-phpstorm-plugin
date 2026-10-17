import { defineConfig } from "@adobe/aio-commerce-lib-app/config";

export default defineConfig({
  metadata: { id: "fixture-order-sync", displayName: "Order synchronization", version: "1.0.0", description: "Reduced ERP integration fixture" },
  eventing: {
    commerce: [{
      provider: { key: "commerce", label: "Commerce", description: "Order changes" },
      events: [{
        name: "observer.sales_order_save_commit_after", label: "Order changed", description: "Forward order changes to ERP",
        fields: [{ name: "increment_id" }],
        runtimeActions: ["order-commerce/updated", "audit/record"],
      }],
    }],
    external: [{
      provider: { key: "backoffice", label: "ERP", description: "ERP order changes" },
      events: [{
        name: "erp.order.updated", label: "ERP order changed", description: "Accept ERP status updates",
        runtimeActions: ["order-backoffice/updated"],
      }],
    }],
  },
});
