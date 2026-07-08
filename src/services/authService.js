import { api } from "./api";

export async function login(payload) {
  // payload contains { email, password }
  return api.post("/auth/login", payload).then((r) => r.data);
}

export async function register(payload) {
  // payload contains { firstName, lastName, email, phone, password, role }
  return api.post("/auth/register", payload).then((r) => r.data);
}

export async function refreshToken(payload) {
  // payload contains { refreshToken }
  return api.post("/auth/refresh", payload).then((r) => r.data);
}

export async function logout() {
  const token = localStorage.getItem("feftms_token");
  if (token) {
    return api.post("/auth/logout", {}, {
      headers: { Authorization: `Bearer ${token}` }
    }).finally(() => {
      localStorage.removeItem("feftms_token");
      localStorage.removeItem("feftms_user");
    });
  }
  return Promise.resolve();
}
