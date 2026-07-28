import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  FiUsers,
  FiShield,
  FiActivity,
  FiSettings,
  FiPlus,
  FiEdit2,
  FiSearch,
  FiCheckCircle,
  FiAlertTriangle,
  FiSave,
  FiKey,
  FiUserCheck,
  FiHome,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import {
  getAdminUsers,
  createAdminUser,
  updateAdminUser,
  updateAdminUserStatus,
  getAdminRoles,
  assignAdminUserRole,
  getAdminAuditHistory,
  getAdminUserActivity,
  getAdminEntityHistory,
  getAdminSettings,
  updateAdminSetting,
} from "../services/adminService";
import "../styles/forms.css";

export const Route = createFileRoute("/admin-management")({
  head: () => ({ meta: [{ title: "System Administration — FEFTMS" }] }),
  component: AdminManagementDash,
});

const SIDE = [
  { key: "users", label: "User Management", icon: FiUsers },
  { key: "roles", label: "Role & Permission", icon: FiShield },
  { key: "audit", label: "Audit Logs", icon: FiActivity },
  { key: "settings", label: "System Settings", icon: FiSettings },
  { key: "back", label: "Main Dashboard", icon: FiHome },
];

function AdminManagementDash() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("users");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Lists State
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [settings, setSettings] = useState([]);

  // Search/Filters
  const [userSearch, setUserSearch] = useState("");
  const [auditSearchUser, setAuditSearchUser] = useState("");
  const [auditSearchModule, setAuditSearchModule] = useState("");

  // Modals & Forms
  const [showUserModal, setShowUserModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [userForm, setUserForm] = useState({
    username: "",
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
    role: "VIEWER",
  });

  const [showRoleModal, setShowRoleModal] = useState(false);
  const [roleUser, setRoleUser] = useState(null);
  const [selectedRoleId, setSelectedRoleId] = useState("");

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      if (activeTab === "users") {
        const uData = await getAdminUsers();
        const rData = await getAdminRoles();
        setUsers(uData.data || []);
        setRoles(rData.data || []);
      } else if (activeTab === "roles") {
        const rData = await getAdminRoles();
        setRoles(rData.data || []);
        const uData = await getAdminUsers();
        setUsers(uData.data || []);
      } else if (activeTab === "audit") {
        const aData = await getAdminAuditHistory();
        setAuditLogs(aData.data || []);
      } else if (activeTab === "settings") {
        const sData = await getAdminSettings();
        setSettings(sData.data || []);
      }
    } catch (err) {
      console.error("Error loading admin data:", err);
      setError(err.response?.data?.message || "Failed to load management data.");
    } finally {
      setLoading(false);
    }
  };

  const handleSaveUser = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!editingUser && userForm.password !== userForm.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    try {
      if (editingUser) {
        await updateAdminUser(editingUser.id, {
          username: userForm.username,
          firstName: userForm.firstName,
          lastName: userForm.lastName,
          email: userForm.email,
          phone: userForm.phone,
        });
        setSuccess("User updated successfully.");
      } else {
        await createAdminUser(userForm);
        setSuccess("User created successfully.");
      }
      setShowUserModal(false);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Operation failed.");
    }
  };

  const handleToggleStatus = async (userId, currentStatus) => {
    setError("");
    setSuccess("");
    const nextStatus = currentStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      await updateAdminUserStatus(userId, nextStatus);
      setSuccess(`User status changed to ${nextStatus}.`);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update status.");
    }
  };

  const handleAssignRole = async (e) => {
    e.preventDefault();
    if (!roleUser || !selectedRoleId) return;
    setError("");
    setSuccess("");
    try {
      await assignAdminUserRole(roleUser.id, selectedRoleId);
      setSuccess("Role assigned successfully.");
      setShowRoleModal(false);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to assign role.");
    }
  };

  const handleUpdateSetting = async (key, value) => {
    setError("");
    setSuccess("");
    try {
      await updateAdminSetting(key, value);
      setSuccess(`Setting '${key}' updated successfully.`);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update setting.");
    }
  };

  const openAddUser = () => {
    setEditingUser(null);
    setUserForm({
      username: "",
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
      role: "VIEWER",
    });
    setShowUserModal(true);
  };

  const openEditUser = (u) => {
    setEditingUser(u);
    setUserForm({
      username: u.username || "",
      firstName: u.firstName || "",
      lastName: u.lastName || "",
      email: u.email || "",
      phone: u.phone || "",
      password: "",
      confirmPassword: "",
      role: u.role || "VIEWER",
    });
    setShowUserModal(true);
  };

  const openRoleAssign = (u) => {
    setRoleUser(u);
    const existing = roles.find((r) => r.roleName === u.role);
    setSelectedRoleId(existing ? String(existing.id) : "");
    setShowRoleModal(true);
  };

  // Filter lists
  const filteredUsers = users.filter((u) => {
    const terms = userSearch.toLowerCase();
    return (
      (u.username || "").toLowerCase().includes(terms) ||
      (u.fullName || "").toLowerCase().includes(terms) ||
      (u.email || "").toLowerCase().includes(terms)
    );
  });

  const filteredAudits = auditLogs.filter((l) => {
    const userMatch = auditSearchUser
      ? (l.username || "").toLowerCase().includes(auditSearchUser.toLowerCase())
      : true;
    const moduleMatch = auditSearchModule
      ? (l.module || "").toLowerCase().includes(auditSearchModule.toLowerCase())
      : true;
    return userMatch && moduleMatch;
  });

  return (
    <RouteGuard allowedRoles={["ADMIN"]}>
      <DashboardLayout
        role="ADMIN"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={(key) => {
          if (key === "back") {
            navigate({ to: "/admin" });
          } else {
            setActiveTab(key);
          }
        }}
      >
        <PageHeader title="System Administration & Auditing" crumbs={["Admin", activeTab]} />

        {error && (
          <div className="fef-alert fef-alert-danger" style={{ display: "flex", gap: "10px", margin: "10px 0" }}>
            <FiAlertTriangle style={{ flexShrink: 0, marginTop: "2px" }} />
            <div>{error}</div>
          </div>
        )}

        {success && (
          <div className="fef-alert fef-alert-success" style={{ display: "flex", gap: "10px", margin: "10px 0" }}>
            <FiCheckCircle style={{ flexShrink: 0, marginTop: "2px" }} />
            <div>{success}</div>
          </div>
        )}

        {/* SECTION: USERS */}
        {activeTab === "users" && (
          <div className="fef-panel">
            <div className="fef-panel-head" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div style={{ display: "flex", gap: "15px", alignItems: "center", flex: 1 }}>
                <div style={{ position: "relative", flex: 1, maxWidth: "350px" }}>
                  <FiSearch style={{ position: "absolute", left: "10px", top: "50%", transform: "translateY(-50%)", color: "var(--feftms-text-muted)" }} />
                  <input
                    type="text"
                    placeholder="Search users..."
                    className="fef-input"
                    style={{ paddingLeft: "35px" }}
                    value={userSearch}
                    onChange={(e) => setUserSearch(e.target.value)}
                  />
                </div>
              </div>
              <button className="fef-btn fef-btn-primary" onClick={openAddUser}>
                <FiPlus style={{ marginRight: "6px" }} /> Create User
              </button>
            </div>

            <div className="fef-table-wrapper">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Username</th>
                    <th>Full Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Last Login</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", padding: "40px" }}>
                        Loading system users...
                      </td>
                    </tr>
                  ) : filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", padding: "40px" }}>
                        No users found matching query.
                      </td>
                    </tr>
                  ) : (
                    filteredUsers.map((u) => (
                      <tr key={u.id}>
                        <td style={{ fontWeight: 600 }}>{u.username}</td>
                        <td>{u.fullName || `${u.firstName || ""} ${u.lastName || ""}`.trim() || "-"}</td>
                        <td>{u.email}</td>
                        <td>
                          <span className={`fef-badge fef-badge-info`}>{u.role}</span>
                        </td>
                        <td>
                          <span className={`fef-badge fef-badge-${u.status === "ACTIVE" ? "success" : "danger"}`}>
                            {u.status}
                          </span>
                        </td>
                        <td>{u.lastLogin ? new Date(u.lastLogin).toLocaleString() : "Never"}</td>
                        <td>
                          <div style={{ display: "flex", gap: "8px" }}>
                            <button
                              className="fef-btn fef-btn-outline"
                              style={{ padding: "4px 8px" }}
                              onClick={() => openEditUser(u)}
                            >
                              <FiEdit2 /> Edit
                            </button>
                            <button
                              className="fef-btn fef-btn-outline"
                              style={{ padding: "4px 8px" }}
                              onClick={() => openRoleAssign(u)}
                            >
                              <FiShield /> Role
                            </button>
                            <button
                              className={`fef-btn fef-btn-${u.status === "ACTIVE" ? "outline-danger" : "outline-success"}`}
                              style={{ padding: "4px 8px" }}
                              onClick={() => handleToggleStatus(u.id, u.status)}
                            >
                              <FiUserCheck /> {u.status === "ACTIVE" ? "Lock" : "Activate"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* SECTION: ROLES & PERMISSIONS */}
        {activeTab === "roles" && (
          <div className="fef-panel">
            <div className="fef-panel-head">
              <h3>Role & Permission Control matrix</h3>
            </div>
            <div style={{ padding: "20px" }}>
              <p style={{ color: "var(--feftms-text-muted)", marginBottom: "20px" }}>
                Below are the mapped roles and permissions stored in the database. Permissions manage administrative and operational access restrictions.
              </p>
              {loading ? (
                <p>Loading database roles and permission states...</p>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                  {roles.map((r) => (
                    <div
                      key={r.id}
                      style={{
                        background: "#f8fafc",
                        border: "1px solid #e2e8f0",
                        borderRadius: "8px",
                        padding: "16px",
                      }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "10px" }}>
                        <h4 style={{ margin: 0, color: "#0f172a", fontSize: "16px" }}>{r.roleName}</h4>
                        <span style={{ fontSize: "12px", color: "var(--feftms-text-muted)" }}>{r.description || "No description"}</span>
                      </div>
                      <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                        {r.permissions && r.permissions.length > 0 ? (
                          r.permissions.map((p) => (
                            <span
                              key={p.id}
                              style={{
                                background: "#e0f2fe",
                                color: "#0369a1",
                                padding: "4px 10px",
                                borderRadius: "4px",
                                fontSize: "11px",
                                fontWeight: 500,
                              }}
                            >
                              {p.permissionName}
                            </span>
                          ))
                        ) : (
                          <span style={{ fontSize: "12px", color: "var(--feftms-text-muted)", fontStyle: "italic" }}>
                            No permissions mapped
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* SECTION: AUDIT LOGS */}
        {activeTab === "audit" && (
          <div className="fef-panel">
            <div className="fef-panel-head" style={{ display: "flex", gap: "15px", flexWrap: "wrap" }}>
              <div style={{ flex: 1, minWidth: "200px" }}>
                <label style={{ fontSize: "12px", display: "block", marginBottom: "4px" }}>Filter User</label>
                <input
                  type="text"
                  placeholder="Username..."
                  className="fef-input"
                  value={auditSearchUser}
                  onChange={(e) => setAuditSearchUser(e.target.value)}
                />
              </div>
              <div style={{ flex: 1, minWidth: "200px" }}>
                <label style={{ fontSize: "12px", display: "block", marginBottom: "4px" }}>Filter Module</label>
                <input
                  type="text"
                  placeholder="Module..."
                  className="fef-input"
                  value={auditSearchModule}
                  onChange={(e) => setAuditSearchModule(e.target.value)}
                />
              </div>
            </div>

            <div className="fef-table-wrapper">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>User</th>
                    <th>Module</th>
                    <th>Action</th>
                    <th>Entity Type</th>
                    <th>Entity ID</th>
                    <th>IP Address</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", padding: "40px" }}>
                        Loading audit activities...
                      </td>
                    </tr>
                  ) : filteredAudits.length === 0 ? (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", padding: "40px" }}>
                        No audit records matched standard filters.
                      </td>
                    </tr>
                  ) : (
                    filteredAudits.map((log) => (
                      <tr key={log.id}>
                        <td>{new Date(log.createdAt || log.timestamp).toLocaleString()}</td>
                        <td style={{ fontWeight: 600 }}>{log.username || log.adminUsername || "SYSTEM"}</td>
                        <td>
                          <span className="fef-badge fef-badge-neutral">{log.module || "SYSTEM"}</span>
                        </td>
                        <td>{log.action}</td>
                        <td>{log.entityType || "-"}</td>
                        <td>{log.entityId || "-"}</td>
                        <td>{log.ipAddress || "-"}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* SECTION: SYSTEM CONFIGURATION */}
        {activeTab === "settings" && (
          <div className="fef-panel" style={{ padding: "20px" }}>
            <h3 style={{ marginBottom: "15px" }}>Dynamic System Configurations</h3>
            <p style={{ color: "var(--feftms-text-muted)", marginBottom: "25px" }}>
              Configure corporate parameters, prefix values, and documentation prefixes. Changing values here takes immediate effect.
            </p>

            {loading ? (
              <p>Loading setting values...</p>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                {settings.map((s) => (
                  <div
                    key={s.id}
                    style={{
                      borderBottom: "1px solid #e2e8f0",
                      paddingBottom: "15px",
                      display: "flex",
                      flexDirection: "column",
                      gap: "8px",
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                      <span style={{ fontWeight: 700, fontSize: "14px", color: "#1e293b" }}>{s.settingKey}</span>
                      <span style={{ fontSize: "11px", color: "var(--feftms-text-muted)" }}>Last updated by: {s.updatedBy}</span>
                    </div>
                    <p style={{ margin: 0, fontSize: "13px", color: "var(--feftms-text-muted)" }}>{s.description}</p>
                    <div style={{ display: "flex", gap: "10px", marginTop: "4px" }}>
                      <input
                        type="text"
                        className="fef-input"
                        style={{ flex: 1 }}
                        defaultValue={s.settingValue}
                        id={`setting-${s.settingKey}`}
                      />
                      <button
                        className="fef-btn fef-btn-primary"
                        onClick={() => {
                          const input = document.getElementById(`setting-${s.settingKey}`);
                          if (input) handleUpdateSetting(s.settingKey, input.value);
                        }}
                      >
                        <FiSave /> Update
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* MODAL: ADD/EDIT USER */}
        {showUserModal && (
          <div className="fef-modal-backdrop" onClick={() => setShowUserModal(false)}>
            <div className="fef-modal-window" style={{ maxWidth: "500px", color: "#1e293b" }} onClick={(e) => e.stopPropagation()}>
              <h2 className="fef-detail-modal-title">{editingUser ? "Edit System User" : "Create User"}</h2>
              <form onSubmit={handleSaveUser} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "15px" }}>
                <div className="fef-field">
                  <label className="fef-label">Username</label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={userForm.username}
                    onChange={(e) => setUserForm({ ...userForm, username: e.target.value })}
                  />
                </div>
                <div style={{ display: "flex", gap: "12px" }}>
                  <div className="fef-field" style={{ flex: 1 }}>
                    <label className="fef-label">First Name</label>
                    <input
                      required
                      type="text"
                      className="fef-input"
                      value={userForm.firstName}
                      onChange={(e) => setUserForm({ ...userForm, firstName: e.target.value })}
                    />
                  </div>
                  <div className="fef-field" style={{ flex: 1 }}>
                    <label className="fef-label">Last Name</label>
                    <input
                      required
                      type="text"
                      className="fef-input"
                      value={userForm.lastName}
                      onChange={(e) => setUserForm({ ...userForm, lastName: e.target.value })}
                    />
                  </div>
                </div>
                <div className="fef-field">
                  <label className="fef-label">Email</label>
                  <input
                    required
                    type="email"
                    className="fef-input"
                    value={userForm.email}
                    onChange={(e) => setUserForm({ ...userForm, email: e.target.value })}
                  />
                </div>
                <div className="fef-field">
                  <label className="fef-label">Phone</label>
                  <input
                    type="text"
                    className="fef-input"
                    value={userForm.phone}
                    onChange={(e) => setUserForm({ ...userForm, phone: e.target.value })}
                  />
                </div>
                {!editingUser && (
                  <>
                    <div className="fef-field">
                      <label className="fef-label">Password</label>
                      <input
                        required
                        type="password"
                        className="fef-input"
                        value={userForm.password}
                        onChange={(e) => setUserForm({ ...userForm, password: e.target.value })}
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Confirm Password</label>
                      <input
                        required
                        type="password"
                        className="fef-input"
                        value={userForm.confirmPassword}
                        onChange={(e) => setUserForm({ ...userForm, confirmPassword: e.target.value })}
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Role</label>
                      <select
                        className="fef-input"
                        value={userForm.role}
                        onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}
                      >
                        {roles.map((r) => (
                          <option key={r.id} value={r.roleName}>
                            {r.roleName}
                          </option>
                        ))}
                      </select>
                    </div>
                  </>
                )}
                <div className="fef-detail-actions" style={{ marginTop: "15px" }}>
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowUserModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="fef-btn fef-btn-primary">
                    {editingUser ? "Save Changes" : "Create"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* MODAL: ASSIGN ROLE */}
        {showRoleModal && (
          <div className="fef-modal-backdrop" onClick={() => setShowRoleModal(false)}>
            <div className="fef-modal-window" style={{ maxWidth: "450px", color: "#1e293b" }} onClick={(e) => e.stopPropagation()}>
              <h2 className="fef-detail-modal-title">Assign User Role</h2>
              <p style={{ color: "var(--feftms-text-muted)", fontSize: "13px" }}>
                Select system role for: <strong>{roleUser?.username}</strong>
              </p>
              <form onSubmit={handleAssignRole} style={{ display: "flex", flexDirection: "column", gap: "15px", marginTop: "15px" }}>
                <div className="fef-field">
                  <label className="fef-label">System Role</label>
                  <select
                    className="fef-input"
                    value={selectedRoleId}
                    onChange={(e) => setSelectedRoleId(e.target.value)}
                  >
                    <option value="">-- Choose Role --</option>
                    {roles.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.roleName}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="fef-detail-actions">
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowRoleModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="fef-btn fef-btn-primary" disabled={!selectedRoleId}>
                    Assign Role
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </DashboardLayout>
    </RouteGuard>
  );
}
