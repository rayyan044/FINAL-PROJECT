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
  (error) => Promise.reject(error)
);

// Global response interceptor (handling token expiration/unauthorized errors)
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("feftms_token");
      localStorage.removeItem("feftms_user");
      // Optionally redirect to login, but we will let components handle state transitions.
    }
    return Promise.reject(error.response?.data || error);
  }
);
