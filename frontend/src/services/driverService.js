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

export async function getDriverAccountStatus(driverId) {
  return api.get(`/drivers/${driverId}/account`).then((r) => r.data);
}

export async function createDriverMobileAccount(driverId, payload) {
  return api.post(`/drivers/${driverId}/account`, payload).then((r) => r.data);
}

export async function resetDriverMobilePassword(driverId) {
  return api.post(`/drivers/${driverId}/account/reset-password`).then((r) => r.data);
}

export async function setDriverMobileAccountEnabled(driverId, enabled) {
  return api.patch(`/drivers/${driverId}/account/${enabled ? "enable" : "disable"}`).then((r) => r.data);
}
