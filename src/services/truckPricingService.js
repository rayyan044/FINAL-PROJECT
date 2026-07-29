import { api } from "./api";

export const listTruckPricing = () => api.get("/truck-pricing").then((response) => response.data);
export const saveTruckPricing = (payload, id) => (id ? api.put(`/truck-pricing/${id}`, payload) : api.post("/truck-pricing", payload)).then((response) => response.data);
