import { api } from "./api";

/**
 * Fetch paginated audit log entries for the admin workspace.
 * @param {Object} params - page and size
 * @returns {Promise<Object>}
 */
export async function listAuditLogs(params = {}) {
  return api.get("/audit-logs", { params }).then((r) => r.data);
}
