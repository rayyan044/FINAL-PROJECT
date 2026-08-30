import { api } from "./api";

export const getCustomerDashboard = () => api.get("/customer-portal/dashboard").then((r) => r.data);
export const getCustomerProfile = () => api.get("/customer-portal/profile").then((r) => r.data);
export const updateCustomerProfile = (payload) => api.put("/customer-portal/profile", payload).then((r) => r.data);
export const listCustomerOrders = () => api.get("/customer-portal/orders").then((r) => r.data);
export const createCustomerOrder = (payload) => api.post("/customer-portal/orders", payload).then((r) => r.data);
export const previewTransportRoute = (payload) => api.post("/transport-route-preview", payload).then((r) => r.data);
export const getCustomerOrder = (id) => api.get(`/customer-portal/orders/${id}`).then((r) => r.data);
export const getCustomerTimeline = (id) => api.get(`/customer-portal/orders/${id}/timeline`).then((r) => r.data);
export const getCustomerDocuments = (id) => api.get(`/customer-portal/orders/${id}/documents`).then((r) => r.data);
export const listCustomerInvoices = () => api.get("/customer-portal/invoices").then((r) => r.data);
export const listCustomerReceipts = () => api.get("/customer-portal/receipts").then((r) => r.data);
export const initiateInvoicePayment = (invoiceId, payment) => api.post(`/customer-portal/invoices/${invoiceId}/pay`, payment).then((r) => r.data);
export const listInvoicePayments = (id) => api.get(`/customer-portal/invoices/${id}/payments`).then((r) => r.data);
export const listCustomerDeliveries = () => api.get("/customer-portal/deliveries").then((r) => r.data);
export const getCustomerDeliveryTracking = (id) => api.get(`/customer-portal/deliveries/${id}/tracking`).then((r) => r.data);
export const downloadCustomerDocument = (path) => api.get(path, { responseType: "blob" });
