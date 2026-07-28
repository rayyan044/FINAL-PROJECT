import React, { useEffect } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useAuth } from "../context/AuthContext";
import { getDashboardForRole } from "../services/roleRoutes";

export function RouteGuard({ allowedRoles, children }) {
  const { user, token, loading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!loading) {
      if (!token || !user) {
        navigate({ to: "/login" });
      } else if (allowedRoles && !allowedRoles.includes(user.role)) {
        const target = getDashboardForRole(user.role);
        navigate({ to: target });
      }
    }
  }, [user, token, loading, navigate, allowedRoles]);

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          height: "100vh",
          alignItems: "center",
          justifyContent: "center",
          background: "var(--feftms-bg)",
          color: "var(--feftms-text)",
          fontSize: "1.1rem",
          fontWeight: 500,
        }}
      >
        Loading workspace...
      </div>
    );
  }

  if (!token || !user || (allowedRoles && !allowedRoles.includes(user.role))) {
    return (
      <div
        style={{
          display: "flex",
          height: "100vh",
          alignItems: "center",
          justifyContent: "center",
          background: "var(--feftms-bg)",
          color: "var(--feftms-text)",
          fontSize: "1.1rem",
          fontWeight: 500,
        }}
      >
        Access Denied. Redirecting...
      </div>
    );
  }

  return children;
}
