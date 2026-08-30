import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";

import { setAuthToken, setUnauthorizedHandler } from "../api/api";
import * as authService from "../services/authService";

const AuthContext = createContext(undefined);
const STORAGE_TIMEOUT_MS = 8_000;

function withTimeout(promise, message) {
  let timeoutId;
  const timeout = new Promise((_, reject) => {
    timeoutId = setTimeout(() => reject(new Error(message)), STORAGE_TIMEOUT_MS);
  });

  return Promise.race([promise, timeout]).finally(() => clearTimeout(timeoutId));
}

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
      // A damaged or stalled device storage operation must not prevent the
      // app from returning to the login screen.
      await withTimeout(authService.logout(), "Session storage did not respond.");
    } catch {
      // The in-memory session still has to be cleared even when device storage
      // is unavailable. A later app start will retry removing the stale values.
    } finally {
      setAuthToken(null);
      setToken(null);
      setUser(null);
    }
  }, []);

  const restoreSession = useCallback(async () => {
    setLoading(true);

    try {
      const session = await withTimeout(authService.loadSession(), "Session storage did not respond.");

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
        await withTimeout(
          authService.saveSession(activeSession.token, activeSession.user, activeSession.refreshToken),
          "Session storage did not respond."
        );
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
