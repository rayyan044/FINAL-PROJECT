import { api } from "./api";

export async function getPendingDispatchActivities() {
  return api.get("/dispatch/pending").then((r) => r.data);
}

export async function createDispatch(loadingActivityId, payload = {}) {
  return api.post(`/dispatch/create/${loadingActivityId}`, payload).then((r) => r.data);
}

export async function getDispatchById(id) {
  return api.get(`/dispatch/${id}`).then((r) => r.data);
}

export async function getDispatchByActivityId(activityId) {
  return api.get(`/dispatch/activity/${activityId}`).then((r) => r.data);
}

export async function releaseTruck(id) {
  return api.post(`/dispatch/${id}/release`).then((r) => r.data);
}

export async function cancelDispatch(id, payload = {}) {
  return api.post(`/dispatch/${id}/cancel`, payload).then((r) => r.data);
}

export async function startTransit(id) {
  return api.post(`/dispatch/${id}/start-transit`).then((r) => r.data);
}
