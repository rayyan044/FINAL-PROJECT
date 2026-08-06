import { api } from "./api";

/**
 * List all payment accounts (paginated).
 * @param {Object} params
 * @returns {Promise<Object>}
 */
export async function listPaymentAccounts(params = {}) {
  return api.get("/payment-accounts", { params }).then((r) => r.data);
}

/**
 * List only active payment accounts.
 * @returns {Promise<Object>}
 */
export async function listActivePaymentAccounts() {
  return api.get("/payment-accounts/active").then((r) => r.data);
}

/**
 * Get payment account details by ID.
 * @param {number|string} id
 * @returns {Promise<Object>}
 */
export async function getPaymentAccountById(id) {
  return api.get(`/payment-accounts/${id}`).then((r) => r.data);
}

/**
 * Create a new payment account.
 * @param {Object} data
 * @returns {Promise<Object>}
 */
export async function createPaymentAccount(data) {
  return api.post("/payment-accounts", data).then((r) => r.data);
}

/**
 * Update an existing payment account.
 * @param {number|string} id
 * @param {Object} data
 * @returns {Promise<Object>}
 */
export async function updatePaymentAccount(id, data) {
  return api.put(`/payment-accounts/${id}`, data).then((r) => r.data);
}

/**
 * Delete a payment account.
 * @param {number|string} id
 * @returns {Promise<Object>}
 */
export async function deletePaymentAccount(id) {
  return api.delete(`/payment-accounts/${id}`).then((r) => r.data);
}

/**
 * Toggle active/inactive status of a payment account.
 * @param {number|string} id
 * @returns {Promise<Object>}
 */
export async function togglePaymentAccountStatus(id) {
  return api.post(`/payment-accounts/${id}/toggle`).then((r) => r.data);
}
