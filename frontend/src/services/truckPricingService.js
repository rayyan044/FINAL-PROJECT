import { api } from "./api";

export const listTruckPricing = () => api.get("/transport-price-ranges").then((response) => response.data);
export const saveTruckPricing = (payload, id) =>
  (id
    ? api.put(`/transport-price-ranges/${id}`, payload)
    : api.post("/transport-price-ranges", payload)
  ).then((response) => response.data);
export const toggleTruckPricing = (id) => api.patch(`/transport-price-ranges/${id}/status`).then((r) => r.data);
export const deleteTruckPricing = (id) => api.delete(`/transport-price-ranges/${id}`).then((r) => r.data);
