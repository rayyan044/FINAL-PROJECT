import { api } from "./api";

/**
 * Fetch dynamic company settings profile.
 * @returns {Promise<Object>}
 */
export async function getCompanySettings() {
  return api.get("/company-settings").then((r) => r.data);
}

/**
 * Update global company settings profile (Admin/Finance).
 * @param {Object} data
 * @returns {Promise<Object>}
 */
export async function updateCompanySettings(data) {
  return api.put("/company-settings", data).then((r) => r.data);
}
