import { api } from "./api";

export const getDashboardSummary = () => api.get("/reports/dashboard");
export const getSalesReport = (from, to) => api.get("/reports/sales", { params: { from, to } });
export const getInventoryReport = () => api.get("/reports/inventory");
export const getLoadingReport = (from, to) => api.get("/reports/loading", { params: { from, to } });
export const getDispatchReport = (from, to) => api.get("/reports/dispatch", { params: { from, to } });
export const getDeliveryReport = (from, to) => api.get("/reports/delivery", { params: { from, to } });
export const getCustomerReport = (customerId) => api.get(`/reports/customer/${customerId}`);
export const getFinancialReport = (from, to) => api.get("/reports/financial", { params: { from, to } });
export const getReportHistory = () => api.get("/reports/history");
