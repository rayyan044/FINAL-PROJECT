import { api } from "./api";
export const previewOrderPricing = (productId, quantity) => api.get("/order-pricing/preview", { params: { productId, quantity } }).then((r) => r.data);
