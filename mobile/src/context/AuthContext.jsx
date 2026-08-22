import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";

import { setAuthToken, setUnauthorizedHandler } from "../api/api";
import * as authService from "../services/authService";

const AuthContext = createContext(undefined);

function isExpiredJwt(token) {
  try {
    const payload = token.split(".")[1];
    if (!payload) return true;

    const normalizedPayload = `${payload.replace(/-/g, "+").replace(/_/g, "/")}${"=".repeat(
      (4 - (payload.length % 4)) % 4
    )}`;
    const decodedPayload = decodeURIComponent(
      atob(normalizedPayload)
        .split("")
        .map((character) => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join("")
    );
    const { exp } = JSON.parse(decodedPayload);

    return typeof exp === "number" && exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const loginInProgress = useRef(false);

  const clearSession = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      setAuthToken(null);
      setToken(null);
      setUser(null);
    }
  }, []);

  const restoreSession = useCallback(async () => {
    setLoading(true);

    try {
      const session = await authService.loadSession();

      if (!session) {
        setAuthToken(null);
        setToken(null);
        setUser(null);
        return;
      }

      let activeSession = session;
      if (isExpiredJwt(session.token)) {
        if (!session.refreshToken) throw new Error("Session expired");
        activeSession = await authService.refreshSession(session.refreshToken);
        await authService.saveSession(activeSession.token, activeSession.user, activeSession.refreshToken);
      }

      setAuthToken(activeSession.token);
      setToken(activeSession.token);
      setUser(activeSession.user);
    } catch {
      await clearSession();
    } finally {
      setLoading(false);
    }
  }, [clearSession]);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  useEffect(() => {
    setUnauthorizedHandler(clearSession);
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  const login = useCallback(async (username, password) => {
    if (loginInProgress.current) return;

    loginInProgress.current = true;
    try {
      const session = await authService.login(username, password);
      await authService.saveSession(session.token, session.user, session.refreshToken);
      setAuthToken(session.token);
      setToken(session.token);
      setUser(session.user);
    } finally {
      loginInProgress.current = false;
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      token,
      loading,
      isAuthenticated: Boolean(token),
      login,
      logout: clearSession,
    }),
    [user, token, loading, login, clearSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider.");
  return context;
}
