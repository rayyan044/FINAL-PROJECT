import React, { createContext, useContext, useState, useEffect } from "react";
import { login as apiLogin, logout as apiLogout } from "../services/authService";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem("feftms_token");
    const storedUser = localStorage.getItem("feftms_user");
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
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
      driverId: data.driverId
    };
    localStorage.setItem("feftms_user", JSON.stringify(userProfile));
    setToken(data.accessToken);
    setUser(userProfile);
    return userProfile;
  };


  const logout = async () => {
    try {
      await apiLogout();
    } catch (e) {
      console.error("Logout API failed", e);
    } finally {
      localStorage.removeItem("feftms_token");
      localStorage.removeItem("feftms_user");
      setToken(null);
      setUser(null);
    }
  };

  const value = {
    token,
    user,
    isAuthenticated: !!token,
    loading,
    login,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
