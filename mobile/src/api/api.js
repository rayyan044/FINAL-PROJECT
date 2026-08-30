import axios from "axios";

// Expo public environment variables are injected at build/start time. Do not
// commit a host fallback: a production build must target an explicit HTTPS API.
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL?.trim();
// Mobile networks can leave a request pending when the backend is unreachable
// (for example, after the development computer gets a new Wi-Fi address).
// Always fail in a predictable time so the UI can offer a retry instead of
// showing a spinner indefinitely.
export const API_REQUEST_TIMEOUT_MS = 15_000;

if (!API_BASE_URL) {
  throw new Error("EXPO_PUBLIC_API_URL must be configured before starting the mobile app.");
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: API_REQUEST_TIMEOUT_MS,
});

let authToken = null;
let unauthorizedHandler = null;

export function setAuthToken(token) {
  authToken = token || null;
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

api.interceptors.request.use((config) => {
  const isLoginRequest = config.url?.includes("/auth/login");

  if (!isLoginRequest && authToken) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${authToken}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = error.config?.url?.includes("/auth/login");

    if (!isLoginRequest && error.response?.status === 401) {
      unauthorizedHandler?.();
    }

    return Promise.reject(error);
  }
);

export default api;
