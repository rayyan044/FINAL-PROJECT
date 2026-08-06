import { api } from "./api";

export async function listInventory(params = {}) {
  return api.get("/inventory", { params }).then((r) => r.data);
}

export async function createInventoryRecord(payload) {
  return api.post("/inventory", payload).then((r) => r.data);
}

export async function listTransactions(params = {}) {
  return api.get("/transactions", { params }).then((r) => r.data);
}

export async function createTransaction(payload) {
  return api.post("/transactions", payload).then((r) => r.data);
}
