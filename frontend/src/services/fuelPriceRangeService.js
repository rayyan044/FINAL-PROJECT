import { api } from "./api";

export const listFuelPriceRanges = () => api.get("/fuel-price-ranges").then((r) => r.data);
export const createFuelPriceRange = (payload) => api.post("/fuel-price-ranges", payload).then((r) => r.data);
export const updateFuelPriceRange = (id, payload) => api.put(`/fuel-price-ranges/${id}`, payload).then((r) => r.data);
export const toggleFuelPriceRange = (id) => api.patch(`/fuel-price-ranges/${id}/status`).then((r) => r.data);
export const deleteFuelPriceRange = (id) => api.delete(`/fuel-price-ranges/${id}`).then((r) => r.data);
