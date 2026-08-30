import AsyncStorage from "@react-native-async-storage/async-storage";

import api from "../api/api";

export const AUTH_TOKEN_KEY = "auth_token";
export const AUTH_USER_KEY = "auth_user";
export const AUTH_REFRESH_TOKEN_KEY = "auth_refresh_token";

function getResponseData(response) {
  return response?.data?.data || response?.data || {};
}

function getToken(data) {
  return data.token || data.accessToken || data.access_token || data.jwtToken || data.jwt;
}

function normalizeUser(data, username) {
  const responseUser = data.user || data.driver || data.profile || {};

  return {
    ...responseUser,
    username: responseUser.username || data.username || username,
    role: responseUser.role || data.role || data.userRole || null,
    // Login and refresh responses are flat TokenResponse objects, while
    // profile responses contain a nested user object. Support both shapes.
    passwordChanged: responseUser.passwordChanged ?? data.passwordChanged ?? true,
  };
}

export function getAuthErrorMessage(error) {
  if (error.code === "ECONNABORTED" || error.code === "ETIMEDOUT") {
    return "The server took too long to respond. Check your connection and try again.";
  }

  if (!error.response) {
    return "Unable to reach the server. Check your internet connection and try again.";
  }

  if (error.response.status === 401 || error.response.status === 403) {
    return "Invalid username or password.";
  }

  if (error.response.status >= 500) {
    return "The server is unavailable right now. Please try again shortly.";
  }

  const message =
    error.response.data?.message ||
    error.response.data?.error ||
    error.response.data?.data?.message;

  const fieldErrors = error.response.data?.data;
  const firstFieldError =
    fieldErrors && typeof fieldErrors === "object"
      ? Object.values(fieldErrors).find((value) => typeof value === "string" && value.trim())
      : null;

  return typeof message === "string" && message.trim()
    ? message
    : firstFieldError ||
      "We could not sign you in. Please try again.";
}

export async function login(username, password) {
  try {
    // The API interceptor deliberately excludes this endpoint from authentication.
    // Current driver accounts authenticate by username. The existing Spring
    // controller calls this request property `email`, even though it accepts a
    // username; including both keeps the mobile client compatible with it and
    // with the username-based controller contract.
    const response = await api.post("/auth/login", {
      username,
      email: username,
      password,
    });
    const data = getResponseData(response);
    const token = getToken(data);

    if (typeof token !== "string" || !token.trim()) {
      throw new Error("The server returned an invalid authentication response.");
    }

    return { token, refreshToken: data.refreshToken || null, user: normalizeUser(data, username) };
  } catch (error) {
    if (error.message === "The server returned an invalid authentication response.") {
      throw error;
    }

    throw new Error(getAuthErrorMessage(error));
  }
}

export async function saveSession(token, user, refreshToken = null) {
  await AsyncStorage.multiSet([
    [AUTH_TOKEN_KEY, token],
    [AUTH_USER_KEY, JSON.stringify(user)],
    [AUTH_REFRESH_TOKEN_KEY, refreshToken || ""],
  ]);
}

export async function loadSession() {
  const entries = await AsyncStorage.multiGet([AUTH_TOKEN_KEY, AUTH_USER_KEY, AUTH_REFRESH_TOKEN_KEY]);
  const values = Object.fromEntries(entries);

  if (!values[AUTH_TOKEN_KEY] || !values[AUTH_USER_KEY]) {
    return null;
  }

  return { token: values[AUTH_TOKEN_KEY], refreshToken: values[AUTH_REFRESH_TOKEN_KEY], user: JSON.parse(values[AUTH_USER_KEY]) };
}

export function saveToken(token) {
  return AsyncStorage.setItem(AUTH_TOKEN_KEY, token);
}

export function loadToken() {
  return AsyncStorage.getItem(AUTH_TOKEN_KEY);
}

export function removeToken() {
  return AsyncStorage.removeItem(AUTH_TOKEN_KEY);
}

export function logout() {
  return AsyncStorage.multiRemove([AUTH_TOKEN_KEY, AUTH_USER_KEY, AUTH_REFRESH_TOKEN_KEY]);
}

export async function refreshSession(refreshToken) {
  const response = await api.post("/auth/refresh", { refreshToken });
  const data = getResponseData(response);
  const token = getToken(data);
  if (!token) throw new Error("The server returned an invalid refresh response.");
  return { token, refreshToken: data.refreshToken || refreshToken, user: normalizeUser(data, data.username || "") };
}

export async function changePassword(password, confirmPassword) {
  try {
    const response = await api.put("/auth/change-password", { password, confirmPassword });
    return getResponseData(response);
  } catch (error) {
    throw new Error(getAuthErrorMessage(error));
  }
}

export async function getCurrentProfile() {
  const response = await api.get("/auth/me");
  const envelope = response.data;
  if (!envelope?.data || typeof envelope.data !== "object") {
    throw new Error("The profile service returned an unexpected response.");
  }
  return envelope.data;
}

export async function getDriverProfile() {
  const response = await api.get("/mobile/profile");
  const envelope = response.data;
  if (!envelope?.data || typeof envelope.data !== "object") {
    throw new Error("The profile service returned an unexpected response.");
  }
  return envelope.data;
}
