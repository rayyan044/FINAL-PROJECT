import { api } from "./api";

/**
 * Fetch paginated invoices.
 * @param {Object} params - page, size, sort
 * @returns {Promise<Object>}
 */
export async function listInvoices(params = {}) {
  return api.get("/invoices", { params }).then((r) => r.data);
}

/**
 * Fetch details of a specific invoice.
 * @param {number|string} id
 * @returns {Promise<Object>}
 */
export async function getInvoiceById(id) {
  return api.get(`/invoices/${id}`).then((r) => r.data);
}

/**
 * Approve/Process payment for an invoice (Finance Desk).
 * @param {number|string} id
 * @returns {Promise<Object>}
 */
export async function approveInvoicePayment(id) {
  return api.post(`/invoices/${id}/approve`).then((r) => r.data);
}

/**
 * Override payment status for an invoice (Admin).
 * @param {number|string} id
 * @param {string} status
 * @returns {Promise<Object>}
 */
export async function overrideInvoiceStatus(id, status) {
  return api.post(`/invoices/${id}/override`, null, { params: { status } }).then((r) => r.data);
}
