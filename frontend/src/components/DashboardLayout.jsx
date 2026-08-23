import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { FiBell, FiLogOut, FiMenu, FiX } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import "../styles/dashboard.css";

export function DashboardLayout({
  role,
  userName,
  pageTitle,
  sideItems,
  activeKey,
  onSelect,
  embedded = false,
  children,
}) {
  const [open, setOpen] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const dropdownRef = useRef(null);
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowProfileMenu(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const displayEmail = user?.email || "";

  const loggedInName =
    user?.firstName && user?.lastName
      ? `${user.firstName} ${user.lastName}`
      : user?.firstName || user?.lastName || "";

  const displayName =
    user?.username || loggedInName || displayEmail.split("@")[0] || userName || "User";

  const formatRole = (rawRole) => {
    if (!rawRole) return "";
    const normalized = String(rawRole).toUpperCase();
    switch (normalized) {
      case "ADMIN":
        return "Administrator";
      case "MANAGER":
        return "Manager";
      case "OPERATIONS":
      case "OPERATOR":
        return "Operations";
      case "SALES_OFFICER":
        return "Sales Officer";
      case "FINANCE":
        return "Finance";
      case "DISPATCHER":
        return "Dispatcher";
      case "DRIVER":
        return "Driver";
      case "CUSTOMER_SERVICE":
        return "Customer Service";
      case "VIEWER":
        return "Viewer";
      case "CUSTOMER":
        return "Customer";
      default:
        return rawRole;
    }
  };

  const displayRoleText = formatRole(user?.role) || formatRole(role);

  const initials = displayName
    .split(" ")
    .map((s) => s[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  const handleLogout = async () => {
    await logout();
    navigate({ to: "/" });
  };

  // Some workflow views are shown as tabs inside a parent workspace. In that
  // case the parent owns the sidebar and top bar, so only render the content.
  if (embedded) {
    return <>{children}</>;
  }

  return (
    <div className="fef-dash">
      <aside className={`fef-sidebar ${open ? "open" : ""}`}>
        <Link to="/" className="fef-brand">
          <span className="fef-brand-mark">F</span>
          <span className="fef-brand-text">FFMS</span>
        </Link>
        <div className="fef-side-heading">Workspace</div>
        {sideItems.map((item) => {
          const Icon = item.icon;
          return (
            <button
              key={item.key}
              className={`fef-side-link ${activeKey === item.key ? "active" : ""}`}
              onClick={() => {
                onSelect?.(item.key);
                setOpen(false);
              }}
            >
              <Icon size={18} /> {item.label}
            </button>
          );
        })}
        <div className="fef-side-heading">Account</div>
        <button className="fef-side-link" onClick={handleLogout}>
          <FiLogOut size={18} /> Logout
        </button>
      </aside>

      <div className="fef-main">
        <header className="fef-topbar">
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <button
              className="fef-icon-btn fef-menu-btn"
              onClick={() => setOpen((s) => !s)}
              aria-label="Toggle menu"
            >
              {open ? <FiX /> : <FiMenu />}
            </button>
            <div className="fef-topbar-title">
              {pageTitle}
              <small>{displayRoleText} workspace</small>
            </div>
          </div>
          <div className="fef-topbar-actions">
            <button className="fef-icon-btn" aria-label="Notifications">
              <FiBell />
              <span className="fef-icon-dot" />
            </button>
            <div className="fef-profile-container" ref={dropdownRef}>
              <div
                className="fef-profile"
                onClick={() => setShowProfileMenu((prev) => !prev)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    setShowProfileMenu((prev) => !prev);
                  }
                }}
                aria-haspopup="true"
                aria-expanded={showProfileMenu}
              >
                <div className="fef-avatar">{initials}</div>
                <div className="fef-profile-text">
                  <div className="fef-profile-name">{displayName}</div>
                  <div className="fef-profile-role">{displayRoleText}</div>
                </div>
              </div>
              {showProfileMenu && (
                <div className="fef-profile-dropdown">
                  <div className="fef-dropdown-header">
                    <div className="fef-dropdown-avatar">{initials}</div>
                    <div className="fef-dropdown-user-info">
                      <div className="fef-dropdown-name">{displayName}</div>
                      <div className="fef-dropdown-email">{displayEmail}</div>
                      <div className="fef-dropdown-role">{displayRoleText}</div>
                    </div>
                  </div>
                  <div className="fef-dropdown-divider"></div>
                  <button className="fef-dropdown-item logout" onClick={handleLogout}>
                    <FiLogOut size={16} />
                    <span>Logout</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="fef-content fef-fade-in">{children}</main>
      </div>
    </div>
  );
}
export function PageHeader({ title, crumbs }) {
  return (
    <div className="fef-page-header">
      <div className="fef-breadcrumb">
        {crumbs.slice(0, -1).map((c) => (
          <span key={c} style={{ color: "var(--feftms-text-muted)", fontWeight: 500 }}>
            {c} /{" "}
          </span>
        ))}
        <span>{crumbs[crumbs.length - 1]}</span>
      </div>
      <h1>{title}</h1>
    </div>
  );
}
export function StatCard({ label, value, trend, icon: Icon, tone = "primary" }) {
  return (
    <div className="fef-stat">
      <div>
        <div className="fef-stat-label">{label}</div>
        <div className="fef-stat-value">{value}</div>
        {trend && <div className="fef-stat-trend">{trend}</div>}
      </div>
      <div className={`fef-stat-icon ${tone}`}>
        <Icon size={22} />
      </div>
    </div>
  );
}
