import { api } from "./api";

export async function listProducts(params = {}) {
  return api.get("/fuel-products", { params }).then((r) => r.data);
}

export async function getProductById(id) {
  return api.get(`/fuel-products/${id}`).then((r) => r.data);
}

export async function createProduct(payload) {
  return api.post("/fuel-products", payload).then((r) => r.data);
}

export async function updateProduct(id, payload) {
  return api.put(`/fuel-products/${id}`, payload).then((r) => r.data);
}

export async function deleteProduct(id) {
  return api.delete(`/fuel-products/${id}`).then((r) => r.data);
}
