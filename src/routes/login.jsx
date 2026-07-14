import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import { useState } from "react";
import { FiLogIn, FiAlertCircle, FiLock, FiEye, FiEyeOff } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import { updateSelfProfile } from "../services/userService";
import "../styles/forms.css";

const ROLE_ROUTES = {
  ADMIN: "/admin",
  SALES_OFFICER: "/sales",
  FINANCE: "/finance",
  OPERATIONS: "/operations",
  DISPATCHER: "/dispatch",
  DRIVER: "/driver",
  CUSTOMER_SERVICE: "/customer-service",
  VIEWER: "/viewer",
  CUSTOMER: "/customer",
};

export const Route = createFileRoute("/login")({
  validateSearch: (search) => ({
    expired: search.expired === "true" || search.expired === true || undefined,
  }),
  head: () => ({
    meta: [
      { title: "Staff Login — FEFTMS" },
      { name: "description", content: "Sign in to the Falcon Energy staff portal." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const { login, updateUser } = useAuth();

  // Login States
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [remember, setRemember] = useState(false);

  // Force Password Change States
  const [forceReset, setForceReset] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [resetError, setResetError] = useState("");
  const [resetLoading, setResetLoading] = useState(false);
  const [tempProfile, setTempProfile] = useState(null);

  // Visibility States
  const [showPassword, setShowPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmNewPassword, setShowConfirmNewPassword] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const userProfile = await login(email, password);
      if (userProfile.passwordChanged === false) {
        setTempProfile(userProfile);
        setForceReset(true);
      } else {
        const path = ROLE_ROUTES[userProfile.role] ?? "/admin";
        navigate({ to: path });
      }
    } catch (err) {
      console.error("Login error object:", err);
      let errMsg = "Invalid email/username or password. Please try again.";
      if (err) {
        if (typeof err === "string") {
          errMsg = err;
        } else if (err.message) {
          errMsg = err.message;
        } else if (err.response?.data?.message) {
          errMsg = err.response.data.message;
        } else if (err.response?.data) {
          errMsg = typeof err.response.data === "string" ? err.response.data : JSON.stringify(err.response.data);
        }
      }
      setError(errMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleForceResetSubmit = async (e) => {
    e.preventDefault();
    setResetError("");

    if (newPassword.length < 6) {
      setResetError("New password must be at least 6 characters long.");
      return;
    }

    if (newPassword !== confirmNewPassword) {
      setResetError("Passwords do not match.");
      return;
    }

    setResetLoading(true);
    try {
      await updateSelfProfile({
        password: newPassword,
        confirmPassword: confirmNewPassword,
      });

      // Update passwordChanged flag in local session
      const updatedProfile = { ...tempProfile, passwordChanged: true };
      updateUser(updatedProfile);

      // Redirect to correct dashboard
      const path = ROLE_ROUTES[updatedProfile.role] ?? "/admin";
      navigate({ to: path });
    } catch (err) {
      console.error(err);
      setResetError(err?.message || "Failed to update password. Please try again.");
    } finally {
      setResetLoading(false);
    }
  };

  if (forceReset) {
    return (
      <div className="fef-login-shell">
        <aside className="fef-login-art fef-fade-in">
          <Link to="/" className="fef-brand" style={{ color: "#fff" }}>
            <span className="fef-brand-mark">F</span>
            <span className="fef-brand-text" style={{ color: "#fff" }}>
              Falcon <span style={{ color: "var(--feftms-secondary)" }}>Energy</span>
            </span>
          </Link>
          <div>
            <h2>Secure Your Account</h2>
            <p>Please update your temporary password to a secure personal password to continue.</p>
          </div>
          <div style={{ opacity: 0.65, fontSize: 13 }}>
            © {new Date().getFullYear()} Falcon Energy
          </div>
        </aside>

        <div className="fef-login-form-wrap">
          <form className="fef-card fef-login-card fef-slide-up" onSubmit={handleForceResetSubmit}>
            <h1>Update Password</h1>
            <p className="fef-sub">Change your initial temporary password.</p>

            {resetError && (
              <div className="fef-alert fef-alert-danger fef-fade-in" style={{ marginBottom: 14 }}>
                <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
                {resetError}
              </div>
            )}

            <div className="fef-field" style={{ marginBottom: 14 }}>
              <label className="fef-label">New Password</label>
              <div className="fef-password-wrapper">
                <input
                  required
                  type={showNewPassword ? "text" : "password"}
                  className="fef-input"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Minimum 6 characters"
                  disabled={resetLoading}
                />
                <button
                  type="button"
                  className="fef-password-toggle"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  disabled={resetLoading}
                  tabIndex="-1"
                >
                  {showNewPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </button>
              </div>
            </div>

            <div className="fef-field" style={{ marginBottom: 20 }}>
              <label className="fef-label">Confirm New Password</label>
              <div className="fef-password-wrapper">
                <input
                  required
                  type={showConfirmNewPassword ? "text" : "password"}
                  className="fef-input"
                  value={confirmNewPassword}
                  onChange={(e) => setConfirmNewPassword(e.target.value)}
                  placeholder="Confirm password"
                  disabled={resetLoading}
                />
                <button
                  type="button"
                  className="fef-password-toggle"
                  onClick={() => setShowConfirmNewPassword(!showConfirmNewPassword)}
                  disabled={resetLoading}
                  tabIndex="-1"
                >
                  {showConfirmNewPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="fef-btn fef-btn-primary fef-btn-block"
              disabled={resetLoading}
            >
              {resetLoading ? (
                "Updating..."
              ) : (
                <>
                  <FiLock /> Update Password & Login
                </>
              )}
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="fef-login-shell">
      <aside className="fef-login-art fef-fade-in">
        <Link to="/" className="fef-brand" style={{ color: "#fff" }}>
          <span className="fef-brand-mark">F</span>
          <span className="fef-brand-text" style={{ color: "#fff" }}>
            Falcon <span style={{ color: "var(--feftms-secondary)" }}>Energy</span>
          </span>
        </Link>
        <div>
          <h2>Run your fuel trading desk from one screen.</h2>
          <p>
            Manage customer requests, loading, dispatch and invoicing — all in real time across your
            team.
          </p>
        </div>
        <div style={{ opacity: 0.65, fontSize: 13 }}>
          © {new Date().getFullYear()} Falcon Energy
        </div>
      </aside>

      <div className="fef-login-form-wrap">
        <form className="fef-card fef-login-card fef-slide-up" onSubmit={onSubmit}>
          <h1>Welcome back</h1>
          <p className="fef-sub">Sign in to your FEFTMS workspace.</p>

          {error && (
            <div className="fef-alert fef-alert-danger fef-fade-in" style={{ marginBottom: 14 }}>
              <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
              {error}
            </div>
          )}

          {search.expired && !error && (
            <div className="fef-alert fef-alert-danger fef-fade-in" style={{ marginBottom: 14 }}>
              <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
              Your session has expired. Please log in again.
            </div>
          )}

          <div className="fef-field" style={{ marginBottom: 14 }}>
            <label className="fef-label">Email Address or Username</label>
            <input
              required
              type="text"
              className="fef-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin or admin@falconenergy.com"
              disabled={loading}
            />
          </div>

          <div className="fef-field" style={{ marginBottom: 14 }}>
            <label className="fef-label">Password</label>
            <div className="fef-password-wrapper">
              <input
                required
                type={showPassword ? "text" : "password"}
                className="fef-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                disabled={loading}
              />
              <button
                type="button"
                className="fef-password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                disabled={loading}
                tabIndex="-1"
              >
                {showPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
              </button>
            </div>
          </div>

          <div className="fef-login-row">
            <label>
              <input
                type="checkbox"
                checked={remember}
                onChange={(e) => setRemember(e.target.checked)}
                disabled={loading}
              />
              Remember me
            </label>
            <a href="#">Forgot password?</a>
          </div>

          <button
            type="submit"
            className="fef-btn fef-btn-primary fef-btn-block"
            disabled={loading}
          >
            {loading ? (
              "Signing in..."
            ) : (
              <>
                <FiLogIn /> Login
              </>
            )}
          </button>



          <p
            style={{
              textAlign: "center",
              marginTop: 18,
              color: "var(--feftms-text-muted)",
              fontSize: 13,
            }}
          >
            Are you a customer?{" "}
            <Link to="/customer" style={{ color: "var(--feftms-secondary)", fontWeight: 600 }}>
              Request Fuel
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
