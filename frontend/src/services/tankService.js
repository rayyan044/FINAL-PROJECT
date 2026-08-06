import { api } from "./api";

export async function listTanks(params = {}) {
  return api.get("/tanks", { params }).then((r) => r.data);
}

export async function getTankById(id) {
  return api.get(`/tanks/${id}`).then((r) => r.data);
}

export async function createTank(payload) {
  return api.post(`/tanks`, payload).then((r) => r.data);
}

export async function updateTank(id, payload) {
  return api.put(`/tanks/${id}`, payload).then((r) => r.data);
}

export async function deleteTank(id) {
  return api.delete(`/tanks/${id}`).then((r) => r.data);
}
