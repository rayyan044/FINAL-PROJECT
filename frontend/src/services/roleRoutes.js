const ROLE_DASHBOARD_ROUTES = {
  ADMIN: "/admin",
  MANAGER: "/reports",
  SALES_OFFICER: "/sales",
  FINANCE: "/finance",
  OPERATIONS: "/operations",
  OPERATOR: "/operations",
  DISPATCHER: "/dispatch",
  DRIVER: "/driver",
  // Customer service has read-only access and uses the Viewer workspace.
  CUSTOMER_SERVICE: "/viewer",
  VIEWER: "/viewer",
  CUSTOMER: "/customer/dashboard",
};

export function getDashboardForRole(role) {
  return ROLE_DASHBOARD_ROUTES[role] || "/";
}
