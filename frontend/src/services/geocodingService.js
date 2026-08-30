import { api } from "./api";

export async function searchLocations(query, signal) {
  if (!query || query.trim().length < 3) return [];
  return api.get("/geocoding/search", { params: { q: query.trim() }, signal }).then((response) => response.data || []);
}

export async function reverseGeocode(latitude, longitude, signal) {
  return api.get("/geocoding/reverse", { params: { latitude, longitude }, signal }).then((response) => response.data);
}
