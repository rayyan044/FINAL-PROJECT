import { createFileRoute, Navigate, Outlet, useRouterState } from "@tanstack/react-router";
import { useAuth } from "../context/AuthContext";

export const Route = createFileRoute("/customer")({
  head: () => ({
    meta: [
      { title: "Create Customer Account — FEFTMS" },
      {
        name: "description",
        content: "Create a customer account before ordering fuel through Falcon Energy.",
      },
    ],
  }),
  component: CustomerEntry,
});

// Retain the former public URL as a safe entry point without exposing a guest
// order form. Customer orders are now always created within the authenticated portal.
function CustomerEntry() {
  const { user } = useAuth();
  const pathname = useRouterState({ select: (state) => state.location.pathname });

  // `customer.dashboard.jsx` is a child route of this legacy URL. Let the
  // child render instead of redirecting the authenticated customer away from it.
  if (pathname === "/customer/dashboard") {
    return <Outlet />;
  }

  if (user?.role === "CUSTOMER") {
    return <Navigate to="/customer/dashboard" />;
  }

  return <Navigate to="/customer-register" />;
}
