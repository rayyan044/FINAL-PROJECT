import React, { useEffect } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useAuth } from "../context/AuthContext";

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
  }, [user, token, loading, navigate]);

  if (loading) {
    return (
      <div style={{ display: "flex", height: "100vh", alignItems: "center", justifyContent: "center", background: "var(--feftms-bg)", color: "var(--feftms-text)", fontSize: "1.1rem", fontWeight: 500 }}>
        Loading workspace...
      </div>
    );
  }

  if (!token || !user || (allowedRoles && !allowedRoles.includes(user.role))) {
    return (
      <div style={{ display: "flex", height: "100vh", alignItems: "center", justifyContent: "center", background: "var(--feftms-bg)", color: "var(--feftms-text)", fontSize: "1.1rem", fontWeight: 500 }}>
        Access Denied. Redirecting...
      </div>
    );
  }

  return children;
}

function getDashboardForRole(role) {
  switch (role) {
    case "ADMIN":
      return "/admin";
    case "OPERATIONS":
    case "OPERATOR":
      return "/operations";
    case "SALES_OFFICER":
      return "/sales";
    case "FINANCE":
      return "/finance";
    case "DISPATCHER":
      return "/dispatch";
    case "DRIVER":
      return "/driver";
    case "VIEWER":
      return "/viewer";
    default:
      return "/";
  }
}
