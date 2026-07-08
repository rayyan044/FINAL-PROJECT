import { api } from "./api";

export async function listUsers(params = {}) {
  return api.get("/users", { params }).then((r) => r.data);
}

export async function getUserById(id) {
  return api.get(`/users/${id}`).then((r) => r.data);
}

export async function createUser(payload) {
  return api.post("/users", payload).then((r) => r.data);
}

export async function updateUser(id, payload) {
  return api.put(`/users/${id}`, payload).then((r) => r.data);
}

export async function deleteUser(id) {
  return api.delete(`/users/${id}`).then((r) => r.data);
}

export async function updateStatus(id, status) {
  return api.patch(`/users/${id}/status`, { status }).then((r) => r.data);
}

export async function resetPassword(id, password, confirmPassword) {
  return api.patch(`/users/${id}/reset-password`, { password, confirmPassword }).then((r) => r.data);
}

export async function updateSelfProfile(payload) {
  return api.put("/users/profile", payload).then((r) => r.data);
}
