import { api } from "./api";

export async function createRequest(payload) {
  // payload: { orderNumber, customerId, productId, quantity, deliveryDate }
  return api.post("/orders", payload).then((r) => r.data);
}

export async function listRequests(params = {}) {
  // params: search, status, customerId, productId, startDate, endDate, page, size, sort
  return api.get("/orders", { params }).then((r) => r.data);
}

export async function getRequestById(id) {
  return api.get(`/orders/${id}`).then((r) => r.data);
}

export async function updateRequest(id, payload) {
  return api.put(`/orders/${id}`, payload).then((r) => r.data);
}

export async function updateRequestStatus(id, status) {
  return api.patch(`/orders/${id}/status`, null, { params: { status } }).then((r) => r.data);
}

export async function approveRequestWithEdit(id, approvedQuantity, reason) {
  return api.post(`/orders/${id}/approve-edited`, { approvedQuantity, reason }).then((r) => r.data);
}

export async function deleteRequest(id) {
  return api.delete(`/orders/${id}`).then((r) => r.data);
}
