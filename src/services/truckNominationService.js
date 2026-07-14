import { api } from "./api";

export async function createNomination(payload) {
  return api.post("/truck-nominations", payload).then((r) => r.data);
}

export async function updateNomination(id, payload) {
  return api.put(`/truck-nominations/${id}`, payload).then((r) => r.data);
}

export async function getNominationById(id) {
  return api.get(`/truck-nominations/${id}`).then((r) => r.data);
}

export async function getNominationByOrderId(orderId) {
  return api.get(`/truck-nominations/order/${orderId}`).then((r) => r.data);
}

export async function submitNomination(id) {
  return api.post(`/truck-nominations/${id}/submit`).then((r) => r.data);
}

export async function rejectNomination(id, reason) {
  return api.post(`/truck-nominations/${id}/reject`, null, { params: { reason } }).then((r) => r.data);
}

export async function approveNomination(id) {
  return api.post(`/truck-nominations/${id}/approve`).then((r) => r.data);
}

export async function cancelNomination(id) {
  return api.post(`/truck-nominations/${id}/cancel`).then((r) => r.data);
}
