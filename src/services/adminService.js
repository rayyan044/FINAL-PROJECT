import { api } from "./api";

// --- Users Management ---
export async function getAdminUsers() {
  return api.get("/admin/users").then((r) => r.data);
}

export async function createAdminUser(payload) {
  return api.post("/admin/users", payload).then((r) => r.data);
}

export async function updateAdminUser(id, payload) {
  return api.put(`/admin/users/${id}`, payload).then((r) => r.data);
}

export async function updateAdminUserStatus(id, status) {
  return api.patch(`/admin/users/${id}/status`, { status }).then((r) => r.data);
}

// --- Roles Management ---
export async function getAdminRoles() {
  return api.get("/admin/roles").then((r) => r.data);
}

export async function assignAdminUserRole(id, roleId) {
  return api.put(`/admin/users/${id}/role`, { roleId }).then((r) => r.data);
}

// --- Audit Logs ---
export async function getAdminAuditHistory() {
  return api.get("/admin/audit").then((r) => r.data);
}

export async function getAdminUserActivity(username) {
  return api.get(`/admin/audit/user/${username}`).then((r) => r.data);
}

export async function getAdminEntityHistory(type, id) {
  return api.get(`/admin/audit/entity/${type}/${id}`).then((r) => r.data);
}

// --- Settings ---
export async function getAdminSettings() {
  return api.get("/admin/settings").then((r) => r.data);
}

export async function updateAdminSetting(key, value) {
  return api.put(`/admin/settings/${key}`, { value }).then((r) => r.data);
}
