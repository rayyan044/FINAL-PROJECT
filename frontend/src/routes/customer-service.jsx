import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";

export const Route = createFileRoute("/customer-service")({
  head: () => ({ meta: [{ title: "Customer Service Desk — FEFTMS" }] }),
  component: CustomerServiceRedirect,
});

function CustomerServiceRedirect() {
  const navigate = useNavigate();
  useEffect(() => {
    navigate({ to: "/viewer" });
  }, [navigate]);

  return (
    <div
      style={{
        display: "flex",
        height: "100vh",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--feftms-bg)",
        color: "var(--feftms-text)",
      }}
    >
      Redirecting to Viewer Dashboard...
    </div>
  );
}
