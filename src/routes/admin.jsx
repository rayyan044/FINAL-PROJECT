import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import {
  FiUsers,
  FiClipboard,
  FiUserCheck,
  FiTruck,
  FiHome,
  FiPlus,
  FiEdit2,
  FiTrash2,
  FiAlertCircle,
  FiCheckCircle,
  FiFileText,
  FiKey,
  FiSearch,
  FiX,
  FiEye,
  FiEyeOff
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { useAuth } from "../context/AuthContext";
import { RouteGuard } from "../components/RouteGuard";
import {
  listUsers,
  createUser,
  updateUser,
  deleteUser,
  updateStatus,
  resetPassword
} from "../services/userService";
import { getAdminDashboardStats } from "../services/dashboardService";
import { listAuditLogs } from "../services/auditService";
import { checkAndSeedDatabase } from "../services/seedService";
import { listRequests } from "../services/requestService";
import "../styles/forms.css";

export const Route = createFileRoute("/admin")({
  head: () => ({ meta: [{ title: "Administrator — FEFTMS" }] }),
  component: AdminDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "users", label: "Users", icon: FiUsers },
  { key: "requests", label: "All Orders", icon: FiClipboard },
  { key: "audit", label: "Audit Logs", icon: FiFileText },
];

function AdminDash() {
  const { user: currentLoggedUser } = useAuth();
  const [activeTab, setActiveTab] = useState("dash");
  const [stats, setStats] = useState({
    customers: 0,
    orders: 0,
    users: 0,
    deliveries: 0,
    activeUsers: 0,
    inactiveUsers: 0,
    salesOfficers: 0
  });

  const [usersList, setUsersList] = useState([]);
  const [ordersList, setOrdersList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Search & Filtering States
  const [search, setSearch] = useState("");
  const [filterRole, setFilterRole] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState("ALL");
  
  // Pagination States
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Audit Logs States
  const [auditLogsList, setAuditLogsList] = useState([]);
  const [auditPage, setAuditPage] = useState(0);
  const [auditTotalPages, setAuditTotalPages] = useState(1);
  const [auditLoading, setAuditLoading] = useState(false);

  // User Form Modal State
  const [showModal, setShowModal] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({
    username: "",
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
    role: "SALES_OFFICER",
    status: "ACTIVE"
  });

  // Password Reset Modal State
  const [resetUser, setResetUser] = useState(null);
  const [resetPasswordVal, setResetPasswordVal] = useState("");
  const [resetConfirmPasswordVal, setResetConfirmPasswordVal] = useState("");
  const [resetModalError, setResetModalError] = useState("");

  // Visibility States
  const [showFormPassword, setShowFormPassword] = useState(false);
  const [showFormConfirmPassword, setShowFormConfirmPassword] = useState(false);
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [showResetConfirmPassword, setShowResetConfirmPassword] = useState(false);

  const loadData = () => {
    setLoading(true);
    setError("");

    checkAndSeedDatabase()
      .then(() => {
        return Promise.all([
          getAdminDashboardStats(),
          listUsers({
            search,
            role: filterRole,
            status: filterStatus,
            page,
            size: 8,
            sort: "id,asc"
          }),
          listRequests({ size: 100 })
        ]);
      })
      .then(([statsData, usersPage, requestsPage]) => {
        setStats({
          customers: statsData.customers || 0,
          orders: statsData.fuelRequests || 0,
          users: statsData.totalUsers || 0,
          deliveries: statsData.deliveries || 0,
          activeUsers: statsData.activeUsers || 0,
          inactiveUsers: statsData.inactiveUsers || 0,
          salesOfficers: statsData.salesOfficers || 0
        });
        setUsersList(usersPage.content || []);
        setOrdersList(requestsPage.content || []);
        setTotalPages(usersPage.totalPages || 1);
        setTotalElements(usersPage.totalElements || 0);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError(err?.message || "Error communicating with PostgreSQL. Ensure backend is running.");
        setLoading(false);
      });
  };

  const loadAuditLogs = (pageIndex = 0) => {
    setAuditLoading(true);
    listAuditLogs({ page: pageIndex, size: 10 })
      .then((res) => {
        setAuditLogsList(res.content || []);
        setAuditTotalPages(res.totalPages || 1);
        setAuditPage(pageIndex);
        setAuditLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Failed to load system audit logs.");
        setAuditLoading(false);
      });
  };

  useEffect(() => {
    if (activeTab === "audit") {
      loadAuditLogs(0);
    } else {
      loadData();
    }
  }, [activeTab, search, filterRole, filterStatus, page]);

  const handleCreateOrUpdate = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (form.username.length < 3) {
      setError("Username must be at least 3 characters long.");
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(form.email)) {
      setError("Please specify a valid email address.");
      return;
    }

    if (!editId) {
      if (form.password.length < 6) {
        setError("Password must be at least 6 characters long.");
        return;
      }
      if (form.password !== form.confirmPassword) {
        setError("Passwords do not match.");
        return;
      }
    }

    try {
      if (editId) {
        // Update user (no password)
        await updateUser(editId, {
          username: form.username,
          firstName: form.firstName,
          lastName: form.lastName,
          phone: form.phone,
          role: form.role,
          status: form.status
        });
        setSuccess("User updated successfully");
      } else {
        // Create user
        await createUser(form);
        setSuccess("User created successfully (forces password change on first login)");
      }
      setShowModal(false);
      setForm({
        username: "",
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        password: "",
        confirmPassword: "",
        role: "SALES_OFFICER",
        status: "ACTIVE"
      });
      setEditId(null);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to save user. Check unique constraints.");
    }
  };

  const handleEditClick = (u) => {
    setEditId(u.id);
    setForm({
      username: u.username,
      firstName: u.firstName,
      lastName: u.lastName,
      email: u.email,
      phone: u.phone || "",
      password: "",
      confirmPassword: "",
      role: u.role,
      status: u.status
    });
    setShowModal(true);
  };

  const handleDeleteClick = async (id) => {
    setError("");
    setSuccess("");

    const targetUser = usersList.find(u => u.id === id);
    if (targetUser && currentLoggedUser && (targetUser.username === currentLoggedUser.username || targetUser.email === currentLoggedUser.email)) {
      setError("You cannot delete your own administrator account.");
      return;
    }

    if (!window.confirm("Are you sure you want to delete this user? This will perform a soft-delete.")) return;
    
    try {
      await deleteUser(id);
      setSuccess("User soft-deleted successfully");
      loadData();
    } catch (err) {
      setError(err?.message || "Cannot delete user.");
    }
  };

  const handleToggleStatus = async (u) => {
    setError("");
    setSuccess("");

    if (currentLoggedUser && (u.username === currentLoggedUser.username || u.email === currentLoggedUser.email)) {
      setError("You cannot deactivate your own account.");
      return;
    }

    const nextStatus = u.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      await updateStatus(u.id, nextStatus);
      setSuccess(`User status toggled to ${nextStatus}`);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to change user status.");
    }
  };

  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    setResetModalError("");

    if (resetPasswordVal.length < 6) {
      setResetModalError("Password must be at least 6 characters.");
      return;
    }

    if (resetPasswordVal !== resetConfirmPasswordVal) {
      setResetModalError("Passwords do not match.");
      return;
    }

    try {
      await resetPassword(resetUser.id, resetPasswordVal, resetConfirmPasswordVal);
      setSuccess(`Password reset successfully for ${resetUser.username}`);
      setResetUser(null);
      setResetPasswordVal("");
      setResetConfirmPasswordVal("");
      loadData();
    } catch (err) {
      setResetModalError(err?.message || "Failed to reset password.");
    }
  };

  return (
    <RouteGuard allowedRoles={["ADMIN"]}>
      <DashboardLayout
        role="Administrator"
      userName="Admin User"
      pageTitle="System Console"
      sideItems={SIDE}
      activeKey={activeTab}
      onSelect={(tab) => {
        setActiveTab(tab);
        setPage(0); // reset page
      }}
    >
      <PageHeader
        title={
          activeTab === "dash"
            ? "Overview"
            : activeTab === "users"
            ? "User Accounts"
            : activeTab === "audit"
            ? "Audit Logs"
            : "All Orders"
        }
        crumbs={["Admin", activeTab]}
      />

      {error && (
        <div className="fef-alert fef-alert-danger fef-fade-in" style={{ marginBottom: 20 }}>
          <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
          {error}
        </div>
      )}

      {success && (
        <div className="fef-alert fef-alert-success fef-fade-in" style={{ marginBottom: 20 }}>
          <FiCheckCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
          {success}
        </div>
      )}

      {/* OVERVIEW TAB */}
      {activeTab === "dash" && (
        <>
          <div className="fef-stat-grid">
            <StatCard label="Total Users" value={stats.users} icon={FiUsers} tone="primary" />
            <StatCard label="Active Accounts" value={stats.activeUsers} icon={FiUserCheck} tone="success" />
            <StatCard label="Inactive Accounts" value={stats.inactiveUsers} icon={FiAlertCircle} tone="secondary" />
            <StatCard label="Total Customers" value={stats.customers} icon={FiUsers} tone="accent" />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 24, marginTop: 24 }}>
            <div className="fef-panel">
              <div className="fef-panel-head">
                <h3>System Users by Role</h3>
              </div>
              <div style={{ padding: 20 }}>
                <table className="fef-table">
                  <thead>
                    <tr>
                      <th>Role</th>
                      <th style={{ textAlign: "right" }}>Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>ADMIN</td>
                      <td style={{ textAlign: "right" }}><strong>{stats.users - stats.salesOfficers}</strong></td>
                    </tr>
                    <tr>
                      <td>SALES_OFFICER</td>
                      <td style={{ textAlign: "right" }}><strong>{stats.salesOfficers}</strong></td>
                    </tr>
                    <tr>
                      <td>Other Workspace Roles</td>
                      <td style={{ textAlign: "right" }}>—</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div className="fef-panel">
              <div className="fef-panel-head">
                <h3>Global Database Stats</h3>
              </div>
              <div style={{ padding: 20 }}>
                <div style={{ marginBottom: 12 }}>
                  <span style={{ color: "var(--feftms-text-muted)" }}>Fuel Requests (Orders):</span>{" "}
                  <strong>{stats.orders}</strong>
                </div>
                <div style={{ marginBottom: 12 }}>
                  <span style={{ color: "var(--feftms-text-muted)" }}>Dispatch Deliveries:</span>{" "}
                  <strong>{stats.deliveries}</strong>
                </div>
                <div>
                  <span style={{ color: "var(--feftms-text-muted)" }}>Active Customers:</span>{" "}
                  <strong>{stats.customers}</strong>
                </div>
              </div>
            </div>
          </div>
        </>
      )}

      {/* USERS MANAGEMENT TAB */}
      {activeTab === "users" && (
        <div className="fef-panel">
          <div className="fef-panel-head" style={{ marginBottom: 20, display: "flex", flexWrap: "wrap", gap: 12, justifyContent: "space-between" }}>
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
              <div style={{ position: "relative", maxWidth: 220 }}>
                <FiSearch style={{ position: "absolute", left: 10, top: 12, color: "var(--feftms-text-muted)" }} />
                <input
                  type="text"
                  placeholder="Search name/email/username..."
                  className="fef-input"
                  style={{ paddingLeft: 30 }}
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                />
              </div>

              <select className="fef-select" style={{ maxWidth: 150 }} value={filterRole} onChange={(e) => { setFilterRole(e.target.value); setPage(0); }}>
                <option value="ALL">All Roles</option>
                <option value="ADMIN">ADMIN</option>
                <option value="SALES_OFFICER">SALES_OFFICER</option>
                <option value="FINANCE">FINANCE</option>
                <option value="OPERATIONS">OPERATIONS</option>
                <option value="DISPATCHER">DISPATCHER</option>
                <option value="DRIVER">DRIVER</option>
                <option value="CUSTOMER_SERVICE">CUSTOMER_SERVICE</option>
                <option value="VIEWER">VIEWER</option>
              </select>

              <select className="fef-select" style={{ maxWidth: 130 }} value={filterStatus} onChange={(e) => { setFilterStatus(e.target.value); setPage(0); }}>
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </div>

            <button
              className="fef-btn fef-btn-primary"
              onClick={() => {
                setEditId(null);
                setForm({
                  username: "",
                  firstName: "",
                  lastName: "",
                  email: "",
                  phone: "",
                  password: "",
                  confirmPassword: "",
                  role: "SALES_OFFICER",
                  status: "ACTIVE"
                });
                setShowModal(true);
              }}
            >
              <FiPlus /> Add New User
            </button>
          </div>

          {/* User Form Modal */}
          {showModal && (
            <div className="fef-card" style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}>
              <h4>{editId ? "Edit User Account" : "Create New User"}</h4>
              <form onSubmit={handleCreateOrUpdate} style={{ marginTop: 15 }}>
                <div className="fef-form-grid">
                  <div className="fef-field">
                    <label className="fef-label">Username</label>
                    <input required className="fef-input" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} disabled={editId && editId === currentLoggedUser?.id} />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">First Name</label>
                    <input required className="fef-input" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Last Name</label>
                    <input required className="fef-input" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Email Address</label>
                    <input required type="email" className="fef-input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} disabled={!!editId} />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Phone</label>
                    <input className="fef-input" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
                  </div>
                  
                  {!editId && (
                    <>
                      <div className="fef-field">
                        <label className="fef-label">Password</label>
                        <div className="fef-password-wrapper">
                          <input
                            required
                            type={showFormPassword ? "text" : "password"}
                            className="fef-input"
                            placeholder="Min 6 characters"
                            value={form.password}
                            onChange={(e) => setForm({ ...form, password: e.target.value })}
                          />
                          <button
                            type="button"
                            className="fef-password-toggle"
                            onClick={() => setShowFormPassword(!showFormPassword)}
                            tabIndex="-1"
                          >
                            {showFormPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                          </button>
                        </div>
                      </div>
                      <div className="fef-field">
                        <label className="fef-label">Confirm Password</label>
                        <div className="fef-password-wrapper">
                          <input
                            required
                            type={showFormConfirmPassword ? "text" : "password"}
                            className="fef-input"
                            placeholder="Confirm password"
                            value={form.confirmPassword}
                            onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                          />
                          <button
                            type="button"
                            className="fef-password-toggle"
                            onClick={() => setShowFormConfirmPassword(!showFormConfirmPassword)}
                            tabIndex="-1"
                          >
                            {showFormConfirmPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                          </button>
                        </div>
                      </div>
                    </>
                  )}

                  <div className="fef-field">
                    <label className="fef-label">System Role</label>
                    <select className="fef-select" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })} disabled={editId && editId === currentLoggedUser?.id}>
                      <option value="ADMIN">ADMIN (Full Control)</option>
                      <option value="SALES_OFFICER">SALES_OFFICER</option>
                      <option value="FINANCE">FINANCE</option>
                      <option value="OPERATIONS">OPERATIONS</option>
                      <option value="DISPATCHER">DISPATCHER</option>
                      <option value="DRIVER">DRIVER</option>
                      <option value="CUSTOMER_SERVICE">CUSTOMER_SERVICE</option>
                      <option value="VIEWER">VIEWER</option>
                    </select>
                  </div>

                  <div className="fef-field">
                    <label className="fef-label">Status</label>
                    <select className="fef-select" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} disabled={editId && editId === currentLoggedUser?.id}>
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                    </select>
                  </div>
                </div>

                <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                  <button type="submit" className="fef-btn fef-btn-primary">Save User</button>
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowModal(false)}>Cancel</button>
                </div>
              </form>
            </div>
          )}

          {/* Reset Password Modal */}
          {resetUser && (
            <div className="fef-card" style={{ padding: 20, marginBottom: 24, border: "1px solid var(--feftms-secondary)", background: "rgba(0,0,0,0.2)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h4>Reset Password for <strong>{resetUser.username}</strong></h4>
                <button className="fef-btn fef-btn-outline" style={{ padding: 4 }} onClick={() => setResetUser(null)}><FiX /></button>
              </div>
              {resetModalError && (
                <div className="fef-alert fef-alert-danger" style={{ margin: "10px 0" }}>
                  {resetModalError}
                </div>
              )}
              <form onSubmit={handleResetPasswordSubmit} style={{ marginTop: 15 }}>
                <div className="fef-form-grid">
                  <div className="fef-field">
                    <label className="fef-label">New Password</label>
                    <div className="fef-password-wrapper">
                      <input
                        required
                        type={showResetPassword ? "text" : "password"}
                        placeholder="Min 6 characters"
                        className="fef-input"
                        value={resetPasswordVal}
                        onChange={(e) => setResetPasswordVal(e.target.value)}
                      />
                      <button
                        type="button"
                        className="fef-password-toggle"
                        onClick={() => setShowResetPassword(!showResetPassword)}
                        tabIndex="-1"
                      >
                        {showResetPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                      </button>
                    </div>
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Confirm Password</label>
                    <div className="fef-password-wrapper">
                      <input
                        required
                        type={showResetConfirmPassword ? "text" : "password"}
                        placeholder="Confirm password"
                        className="fef-input"
                        value={resetConfirmPasswordVal}
                        onChange={(e) => setResetConfirmPasswordVal(e.target.value)}
                      />
                      <button
                        type="button"
                        className="fef-password-toggle"
                        onClick={() => setShowResetConfirmPassword(!showResetConfirmPassword)}
                        tabIndex="-1"
                      >
                        {showResetConfirmPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                      </button>
                    </div>
                  </div>
                </div>
                <div style={{ marginTop: 15, display: "flex", gap: 10 }}>
                  <button type="submit" className="fef-btn fef-btn-primary">Change Password</button>
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setResetUser(null)}>Cancel</button>
                </div>
              </form>
            </div>
          )}

          {/* Users Table */}
          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Full Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {usersList.map((u) => {
                  const isSelf = currentLoggedUser && (u.username === currentLoggedUser.username || u.email === currentLoggedUser.email);
                  return (
                    <tr key={u.id}>
                      <td><strong>{u.username}</strong> {isSelf && <span style={{ fontSize: 10, opacity: 0.6 }}>(You)</span>}</td>
                      <td>{u.firstName} {u.lastName}</td>
                      <td>{u.email}</td>
                      <td>{u.phone || "—"}</td>
                      <td><span className="fef-badge fef-badge-progress">{u.role}</span></td>
                      <td>
                        <button
                          className={`fef-badge fef-badge-${u.status === "ACTIVE" ? "approved" : "pending"}`}
                          style={{ border: "none", cursor: isSelf ? "not-allowed" : "pointer" }}
                          disabled={isSelf}
                          onClick={() => handleToggleStatus(u)}
                          title={isSelf ? "Self deactivation disabled" : "Click to toggle status"}
                        >
                          {u.status}
                        </button>
                      </td>
                      <td>
                        <button
                          className="fef-btn fef-btn-outline"
                          style={{ padding: "4px 8px", marginRight: 8 }}
                          onClick={() => handleEditClick(u)}
                          title="Edit User Details"
                        >
                          <FiEdit2 />
                        </button>

                        <button
                          className="fef-btn fef-btn-outline"
                          style={{ padding: "4px 8px", marginRight: 8 }}
                          onClick={() => setResetUser(u)}
                          title="Reset User Password"
                        >
                          <FiKey />
                        </button>

                        <button
                          className="fef-btn fef-btn-danger"
                          style={{ padding: "4px 8px", opacity: isSelf ? 0.3 : 1, cursor: isSelf ? "not-allowed" : "pointer" }}
                          disabled={isSelf}
                          onClick={() => handleDeleteClick(u.id)}
                          title={isSelf ? "Cannot delete yourself" : "Soft-delete User"}
                        >
                          <FiTrash2 />
                        </button>
                      </td>
                    </tr>
                  );
                })}
                {usersList.length === 0 && (
                  <tr>
                    <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                      No matching user accounts found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Users Pagination */}
          {totalPages > 1 && (
            <div style={{ marginTop: 20, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>
                Showing page {page + 1} of {totalPages} ({totalElements} users)
              </span>
              <div style={{ display: "flex", gap: 8 }}>
                <button
                  className="fef-btn fef-btn-outline"
                  style={{ padding: "4px 12px" }}
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  Previous
                </button>
                <button
                  className="fef-btn fef-btn-outline"
                  style={{ padding: "4px 12px" }}
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ALL ORDERS RECORDS TAB */}
      {activeTab === "requests" && (
        <div className="fef-panel">
          <div style={{ padding: "20px 0", color: "var(--feftms-text-muted)" }}>
            Please approve or reject customer requests in the <strong>Sales Desk Dashboard</strong>. Complete logs are displayed below.
          </div>
          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Customer</th>
                  <th>Product</th>
                  <th>Qty (L)</th>
                  <th>Calculated Price</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {ordersList.map((o) => (
                  <tr key={o.id}>
                    <td>{o.orderNumber}</td>
                    <td>
                      <div>{o.customerName}</div>
                      {o.emergencyLevel && (
                        <span 
                          className={`fef-badge fef-badge-${o.emergencyLevel.toLowerCase().includes('critical') ? 'danger' : 'info'}`}
                          style={{ fontSize: 10, padding: '2px 6px', marginTop: 4, display: 'inline-block' }}
                        >
                          {o.emergencyLevel}
                        </span>
                      )}
                      {o.driverName && (
                        <div style={{ fontSize: 11, color: 'var(--feftms-text-muted)', marginTop: 4 }}>
                          Driver: {o.driverName} ({o.driverPhone})
                        </div>
                      )}
                      {o.locationAddress && (
                        <div style={{ fontSize: 10, color: 'var(--feftms-text-muted)', marginTop: 2 }} title={`GPS: ${o.locationGps || 'N/A'} | Landmark: ${o.locationLandmark || 'N/A'}`}>
                          📍 {o.locationAddress}
                        </div>
                      )}
                    </td>
                    <td>{o.productName}</td>
                    <td>{o.quantity?.toLocaleString()}</td>
                    <td>${o.amount?.toLocaleString()}</td>
                    <td>
                      <span className={`fef-badge fef-badge-${o.orderStatus?.toLowerCase()}`}>
                        {o.orderStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* AUDIT LOGS TAB */}
      {activeTab === "audit" && (
        <div className="fef-panel">
          {auditLoading ? (
            <div style={{ padding: 40, textAlign: "center", color: "var(--feftms-text-muted)" }}>
              Loading system audit logs...
            </div>
          ) : (
            <>
              <div className="fef-table-wrap">
                <table className="fef-table">
                  <thead>
                    <tr>
                      <th>Timestamp</th>
                      <th>Admin User</th>
                      <th>Administrative Action</th>
                      <th>Affected Username</th>
                      <th>IP Address</th>
                      <th>Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogsList.map((l) => (
                      <tr key={l.id}>
                        <td style={{ fontSize: 13, whiteSpace: "nowrap" }}>
                          {new Date(l.timestamp).toLocaleString()}
                        </td>
                        <td>
                          <strong>{l.adminUsername}</strong>{" "}
                          <span style={{ fontSize: 10, opacity: 0.6 }}>(ID: {l.adminId || "SYS"})</span>
                        </td>
                        <td>
                          <span className="fef-badge fef-badge-progress">{l.action}</span>
                        </td>
                        <td>
                          {l.affectedUsername ? <strong>{l.affectedUsername}</strong> : "—"}
                        </td>
                        <td>
                          <code style={{ fontSize: 12 }}>{l.ipAddress}</code>
                        </td>
                        <td style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>
                          {l.details || "—"}
                        </td>
                      </tr>
                    ))}
                    {auditLogsList.length === 0 && (
                      <tr>
                        <td colSpan="6" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                          No administrative action records logged in database.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              {/* Audit Logs Pagination */}
              {auditTotalPages > 1 && (
                <div style={{ marginTop: 20, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>
                    Showing page {auditPage + 1} of {auditTotalPages}
                  </span>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button
                      className="fef-btn fef-btn-outline"
                      style={{ padding: "4px 12px" }}
                      disabled={auditPage === 0}
                      onClick={() => loadAuditLogs(auditPage - 1)}
                    >
                      Previous
                    </button>
                    <button
                      className="fef-btn fef-btn-outline"
                      style={{ padding: "4px 12px" }}
                      disabled={auditPage >= auditTotalPages - 1}
                      onClick={() => loadAuditLogs(auditPage + 1)}
                    >
                      Next
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}
      </DashboardLayout>
    </RouteGuard>
  );
}
