import { api } from "./api";

export async function listDrivers(params = {}) {
  return api.get("/drivers", { params }).then((r) => r.data);
}

export async function getDriverById(id) {
  return api.get(`/drivers/${id}`).then((r) => r.data);
}

export async function createDriver(payload) {
  return api.post("/drivers", payload).then((r) => r.data);
}

export async function updateDriver(id, payload) {
  return api.put(`/drivers/${id}`, payload).then((r) => r.data);
}

export async function deleteDriver(id) {
  return api.delete(`/drivers/${id}`).then((r) => r.data);
}
