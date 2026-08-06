import React, { createContext, useContext, useState, useEffect } from "react";
import { login as apiLogin, logout as apiLogout } from "../services/authService";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import { registerLogoutCallback } from "../services/api";

const SUPPORTED_ROLES = new Set([
  "ADMIN",
  "MANAGER",
  "SALES_OFFICER",
  "FINANCE",
  "OPERATIONS",
  "OPERATOR",
  "DISPATCHER",
  "DRIVER",
  "CUSTOMER_SERVICE",
  "VIEWER",
  "CUSTOMER",
]);

const AuthContext = createContext(null);

function parseStoredUser(json) {
  try {
    return JSON.parse(json);
  } catch (err) {
    console.warn("Failed to parse stored user profile", err);
    sessionStorage.removeItem("feftms_user");
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();
  const router = useRouter();

  useEffect(() => {
    const restoreSession = () => {
      const storedToken = sessionStorage.getItem("feftms_token");
      const storedUser = sessionStorage.getItem("feftms_user");
      const parsedUser = storedUser ? parseStoredUser(storedUser) : null;

      if (storedToken && parsedUser) {
        setToken(storedToken);
        setUser(parsedUser);
      } else {
        setToken(null);
        setUser(null);
        sessionStorage.removeItem("feftms_token");
        sessionStorage.removeItem("feftms_user");
      }
      setLoading(false);
    };

    restoreSession();

    // Register callback for 401 response interceptor
    registerLogoutCallback(() => {
      setToken(null);
      setUser(null);
      queryClient.clear();
      router.invalidate();
      router.navigate({ to: "/login", search: { expired: true } });
    });
  }, [queryClient, router]);

  const updateUser = (updatedProfile) => {
    sessionStorage.setItem("feftms_user", JSON.stringify(updatedProfile));
    setUser(updatedProfile);
  };

  const login = async (email, password) => {
    setLoading(true);
    // Clear any previous session and cache data before logging in
    queryClient.clear();
    sessionStorage.removeItem("feftms_token");
    sessionStorage.removeItem("feftms_user");

    try {
      const data = await apiLogin({ email, password });
      if (
        !data ||
        typeof data.accessToken !== "string" ||
        !data.accessToken.trim() ||
        typeof data.email !== "string" ||
        !data.email.trim() ||
        typeof data.username !== "string" ||
        !data.username.trim() ||
        typeof data.role !== "string" ||
        !SUPPORTED_ROLES.has(data.role)
      ) {
        throw new Error(
          "The authentication service returned incomplete account information. Please try again.",
        );
      }
      sessionStorage.setItem("feftms_token", data.accessToken);
      const userProfile = {
        email: data.email,
        username: data.username,
        role: data.role,
        passwordChanged: data.passwordChanged,
        phone: data.phone,
        firstName: data.firstName,
        lastName: data.lastName,
        driverId: data.driverId,
      };
      sessionStorage.setItem("feftms_user", JSON.stringify(userProfile));
      setToken(data.accessToken);
      setUser(userProfile);
      setLoading(false);
      return userProfile;
    } catch (error) {
      setToken(null);
      setUser(null);
      setLoading(false);
      throw error;
    }
  };

  const logout = async () => {
    setLoading(true);
    const currentToken = token || sessionStorage.getItem("feftms_token");

    // Instantly clear the storage, state, and query cache for immediate UI transition
    sessionStorage.removeItem("feftms_token");
    sessionStorage.removeItem("feftms_user");
    setToken(null);
    setUser(null);
    queryClient.clear();

    try {
      await apiLogout(currentToken);
    } catch (e) {
      console.error("Logout API failed", e);
    } finally {
      setLoading(false);
      router.invalidate();
    }
  };

  const value = {
    token,
    user,
    isAuthenticated: !!token,
    loading,
    login,
    logout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{!loading && children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
