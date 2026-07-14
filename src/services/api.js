import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api/v1",
  headers: { "Content-Type": "application/json" },
});

// Inject Bearer token from localStorage on every request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("feftms_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

let logoutCallback = null;

export const registerLogoutCallback = (cb) => {
  logoutCallback = cb;
};

// Global response interceptor (handling token expiration/unauthorized errors)
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      const requestToken = error.config?.headers?.Authorization?.replace("Bearer ", "");
      const currentToken = localStorage.getItem("feftms_token");
      if (requestToken && requestToken === currentToken) {
        localStorage.removeItem("feftms_token");
        localStorage.removeItem("feftms_user");
        if (logoutCallback) {
          logoutCallback();
        }
      }
    }
    return Promise.reject(error.response?.data || error);
  },
);
