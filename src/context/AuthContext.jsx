import React, { createContext, useContext, useState, useEffect } from "react";
import { login as apiLogin, logout as apiLogout } from "../services/authService";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import { registerLogoutCallback } from "../services/api";

const AuthContext = createContext(null);

function parseStoredUser(json) {
  try {
    return JSON.parse(json);
  } catch (err) {
    console.warn("Failed to parse stored user profile", err);
    localStorage.removeItem("feftms_user");
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
      const storedToken = localStorage.getItem("feftms_token");
      const storedUser = localStorage.getItem("feftms_user");
      const parsedUser = storedUser ? parseStoredUser(storedUser) : null;

      if (storedToken && parsedUser) {
        setToken(storedToken);
        setUser(parsedUser);
      } else {
        setToken(null);
        setUser(null);
        localStorage.removeItem("feftms_token");
        localStorage.removeItem("feftms_user");
      }
      setLoading(false);
    };

    const handleStorageChange = (event) => {
      if (event.key === "feftms_token" || event.key === "feftms_user") {
        restoreSession();
      }
    };

    restoreSession();
    window.addEventListener("storage", handleStorageChange);

    // Register callback for 401 response interceptor
    registerLogoutCallback(() => {
      setToken(null);
      setUser(null);
      queryClient.clear();
      router.invalidate();
      router.navigate({ to: "/login", search: { expired: true } });
    });

    return () => {
      window.removeEventListener("storage", handleStorageChange);
    };
  }, [queryClient, router]);

  const updateUser = (updatedProfile) => {
    localStorage.setItem("feftms_user", JSON.stringify(updatedProfile));
    setUser(updatedProfile);
  };

  const login = async (email, password) => {
    setLoading(true);
    // Clear any previous session and cache data before logging in
    queryClient.clear();
    localStorage.removeItem("feftms_token");
    localStorage.removeItem("feftms_user");

    try {
      const data = await apiLogin({ email, password });
      localStorage.setItem("feftms_token", data.accessToken);
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
      localStorage.setItem("feftms_user", JSON.stringify(userProfile));
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
    const currentToken = token || localStorage.getItem("feftms_token");

    // Instantly clear the storage, state, and query cache for immediate UI transition
    localStorage.removeItem("feftms_token");
    localStorage.removeItem("feftms_user");
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
