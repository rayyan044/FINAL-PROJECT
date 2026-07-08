import { api } from "./api";

export async function listCustomers(params = {}) {
  return api.get("/customers", { params }).then((r) => r.data);
}

export async function getCustomerById(id) {
  return api.get(`/customers/${id}`).then((r) => r.data);
}

export async function createCustomer(payload) {
  return api.post("/customers", payload).then((r) => r.data);
}

export async function updateCustomer(id, payload) {
  return api.put(`/customers/${id}`, payload).then((r) => r.data);
}

export async function deleteCustomer(id) {
  return api.delete(`/customers/${id}`).then((r) => r.data);
}
