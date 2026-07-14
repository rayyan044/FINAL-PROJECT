import { api } from "./api";

export async function createLoadingOrder(payload) {
  return api.post("/loading-orders", payload).then((r) => r.data);
}

export async function updateLoadingOrder(id, payload) {
  return api.put(`/loading-orders/${id}`, payload).then((r) => r.data);
}

export async function getLoadingOrderById(id) {
  return api.get(`/loading-orders/${id}`).then((r) => r.data);
}

export async function getLoadingOrderByOrderId(orderId) {
  return api.get(`/loading-orders/order/${orderId}`).then((r) => r.data);
}

export async function listLoadingOrders() {
  return api.get("/loading-orders").then((r) => r.data);
}

export async function approveLoadingOrder(id) {
  return api.post(`/loading-orders/${id}/approve`).then((r) => r.data);
}

export async function cancelLoadingOrder(id) {
  return api.post(`/loading-orders/${id}/cancel`).then((r) => r.data);
}

export async function startLoadingActivity(id, activityId, params = {}) {
  return api.post(`/loading-orders/${id}/activities/${activityId}/start`, null, { params }).then((r) => r.data);
}

export async function completeLoadingActivity(id, activityId) {
  return api.post(`/loading-orders/${id}/activities/${activityId}/complete`).then((r) => r.data);
}
