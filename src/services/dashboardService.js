import { api } from "./api";

/**
 * Fetch consolidated statistics for the admin dashboard.
 * @returns {Promise<Object>}
 */
export async function getAdminDashboardStats() {
  return api.get("/dashboard/admin").then((r) => r.data);
}
