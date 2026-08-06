import { api } from "./api";

export async function listOrders(params = {}) {
  return api.get("/orders", { params }).then((r) => r.data);
}

export async function getOrderById(id) {
  return api.get(`/orders/${id}`).then((r) => r.data);
}

export async function createOrder(payload) {
  return api.post(`/orders`, payload).then((r) => r.data);
}

export async function updateOrder(id, payload) {
  return api.put(`/orders/${id}`, payload).then((r) => r.data);
}

export async function updateOrderStatus(id, status) {
  return api.patch(`/orders/${id}/status`, null, { params: { status } }).then((r) => r.data);
}

export async function deleteOrder(id) {
  return api.delete(`/orders/${id}`).then((r) => r.data);
}
