import { api } from "./api";

export async function listDeliveries(params = {}) {
  return api.get("/deliveries", { params }).then((r) => r.data);
}

export async function getDeliveryById(id) {
  return api.get(`/deliveries/${id}`).then((r) => r.data);
}

export async function createDelivery(payload) {
  return api.post("/deliveries", payload).then((r) => r.data);
}

export async function updateDelivery(id, payload) {
  return api.put(`/deliveries/${id}`, payload).then((r) => r.data);
}

export async function updateDeliveryStatus(id, status) {
  return api.patch(`/deliveries/${id}/status`, null, { params: { status } }).then((r) => r.data);
}

export async function deleteDelivery(id) {
  return api.delete(`/deliveries/${id}`).then((r) => r.data);
}
