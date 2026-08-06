import axios from "axios";

// Environment files provide VITE_API_BASE_URL for local, staging, and production.
// The relative fallback supports a frontend and API served through the same proxy.
const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const apiBaseUrl = (configuredApiBaseUrl || "/api/v1").replace(/\/$/, "");

export class ApiRequestError extends Error {
  constructor(message, { status, code, cause } = {}) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.code = code;
    this.cause = cause;
  }
}

/**
 * Converts Axios and API-envelope failures into safe messages suitable for the UI.
 * The original error remains attached for browser-console diagnostics only.
 */
export function normalizeApiError(error) {
  if (error instanceof ApiRequestError) return error;

  const status = error?.response?.status;
  const payload = error?.response?.data;
  const backendMessage =
    payload && typeof payload === "object" && typeof payload.message === "string"
      ? payload.message.trim().toLowerCase()
      : "";

  let message;
  if (!status) {
    message =
      error?.code === "ECONNABORTED" || /timeout/i.test(error?.message || "")
        ? "The request timed out. Please try again."
        : "Unable to reach the service. Check your network connection. If this page is hosted separately from the API, the connection may be blocked by CORS.";
  } else if (status === 400) {
    message = backendMessage || "The request was invalid. Please check the entered details and try again.";
  } else if (status === 401) {
    message = backendMessage.includes("expired")
      ? "This account or its credentials have expired. Please contact an administrator."
      : "Invalid email or password.";
  } else if (status === 403) {
    message = backendMessage.includes("lock")
      ? "This account is locked. Please contact an administrator."
      : backendMessage.includes("inactive") || backendMessage.includes("disabled")
        ? "This account is inactive. Please contact an administrator."
        : "You do not have permission to access this service.";
  } else if (status === 404) {
    message = "The authentication service could not be found. Please contact support.";
  } else if (status >= 500) {
    message = "The service is temporarily unavailable. Please try again later.";
  } else {
    message = backendMessage || "The request could not be completed. Please try again.";
  }

  return new ApiRequestError(message, { status, code: error?.code, cause: error });
}

export const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
    "Cache-Control": "no-cache, no-store, must-revalidate",
    Pragma: "no-cache",
    Expires: "0",
  },
});

// Inject Bearer token from sessionStorage on every request
api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem("feftms_token");
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
  (response) => {
    const envelope = response.data;
    // API clients receive the project's ApiResponse envelope. Treat an explicit
    // unsuccessful envelope as an error even if a proxy returned HTTP 200.
    if (envelope && typeof envelope === "object" && envelope.success === false) {
      return Promise.reject(
        new ApiRequestError(envelope.message || "The request could not be completed.", {
          status: response.status,
        }),
      );
    }
    return envelope;
  },
  (error) => {
    if (error.response?.status === 401) {
      const requestToken = error.config?.headers?.Authorization?.replace("Bearer ", "");
      const currentToken = sessionStorage.getItem("feftms_token");
      const isLoginRequest = error.config?.url?.includes("/auth/login");
      if (!isLoginRequest && requestToken && requestToken === currentToken) {
        sessionStorage.removeItem("feftms_token");
        sessionStorage.removeItem("feftms_user");
        if (logoutCallback) {
          logoutCallback();
        }
      }
    }
    return Promise.reject(normalizeApiError(error));
  },
);
