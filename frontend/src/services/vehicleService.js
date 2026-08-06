import { api } from "./api";

export async function listVehicles(params = {}) {
  return api.get("/vehicles", { params }).then((r) => r.data);
}

export async function getVehicleById(id) {
  return api.get(`/vehicles/${id}`).then((r) => r.data);
}

export async function createVehicle(payload) {
  return api.post("/vehicles", payload).then((r) => r.data);
}

export async function updateVehicle(id, payload) {
  return api.put(`/vehicles/${id}`, payload).then((r) => r.data);
}

export async function deleteVehicle(id) {
  return api.delete(`/vehicles/${id}`).then((r) => r.data);
}
