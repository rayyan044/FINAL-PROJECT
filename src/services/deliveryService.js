import { api } from "./api";

export async function getActiveDeliveries() {
  return api.get("/deliveries/active").then((r) => r.data);
}

export async function createDeliveryRecord(dispatchId) {
  return api.post(`/deliveries/create/${dispatchId}`).then((r) => r.data);
}

export async function getDeliveryById(id) {
  return api.get(`/deliveries/${id}`).then((r) => r.data);
}

export async function recordArrival(id, payload) {
  return api.post(`/deliveries/${id}/arrival`, payload).then((r) => r.data);
}

export async function completeDelivery(id, payload) {
  return api.post(`/deliveries/${id}/complete`, payload).then((r) => r.data);
}

export async function getDeliveryHistory() {
  return api.get("/deliveries/history").then((r) => r.data);
}

// Keep stubs for backwards compatibility if needed
export async function listDeliveries(params = {}) {
  return api.get("/deliveries/active").then((r) => r.data);
}
export async function updateDeliveryStatus(id, status) {
  if (status === "ARRIVED") {
    return recordArrival(id, { receivedBy: "operations", remarks: "Updated trip status" });
  } else if (status === "DELIVERED") {
    return completeDelivery(id, { completedBy: "operations", remarks: "Updated trip status" });
  }
  return api.patch(`/deliveries/${id}/status`, null, { params: { status } }).then((r) => r.data);
}

export const createDelivery = createDeliveryRecord;
export async function updateDelivery(id, payload) {
  return api.put(`/deliveries/${id}`, payload).then((r) => r.data);
}
