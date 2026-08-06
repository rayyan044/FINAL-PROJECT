import { api, ApiRequestError } from "./api";

export async function login(payload) {
  // payload contains { email, password }
  const response = await api.post("/auth/login", payload);
  if (
    !response ||
    response.success !== true ||
    !response.data ||
    typeof response.data !== "object"
  ) {
    throw new ApiRequestError(
      "The authentication service returned an invalid response. Please try again.",
    );
  }
  return response.data;
}

export async function register(payload) {
  // payload contains { firstName, lastName, email, phone, password, role }
  return api.post("/auth/register", payload).then((r) => r.data);
}

export async function refreshToken(payload) {
  // payload contains { refreshToken }
  return api.post("/auth/refresh", payload).then((r) => r.data);
}

export async function logout(passedToken) {
  const token = passedToken || sessionStorage.getItem("feftms_token");
  if (token) {
    return api
      .post(
        "/auth/logout",
        {},
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      )
      .then((r) => r.data)
      .finally(() => {
        sessionStorage.removeItem("feftms_token");
        sessionStorage.removeItem("feftms_user");
      });
  }
  return Promise.resolve();
}
