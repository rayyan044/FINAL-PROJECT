import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { registerCustomer } from "../services/authService";
import { SiteNav } from "../components/SiteNav";
import "../styles/forms.css";

export const Route = createFileRoute("/customer-register")({ component: CustomerRegister });
const isTanzanianMobile = (value) => /^[67]\d{8}$/.test(value);

function CustomerRegister() {
  const navigate = useNavigate(),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    const x = Object.fromEntries(new FormData(e.currentTarget));
    for (const field of ["companyPhone", "phone"]) {
      x[field] = String(x[field] || "").replace(/\D/g, "");
      if (x[field] && !isTanzanianMobile(x[field])) {
        setError("Enter a 9-digit Tanzanian mobile number starting with 6 or 7.");
        setSaving(false);
        return;
      }
    }
    try {
      await registerCustomer(x);
      navigate({ to: "/login", search: { customer: true, registered: true } });
    } catch (err) {
      setError(err.message || "Registration could not be completed.");
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="fef-page">
      <SiteNav />
      <main className="fef-wizard-container" style={{ maxWidth: 760 }}>
        <div className="fef-form-head">
          <h1>Create customer account</h1>
          <p>Register a company account to order and track fuel securely.</p>
        </div>
        {error && <div className="fef-alert fef-alert-danger">{error}</div>}
        <form className="fef-panel" onSubmit={submit} style={{ padding: 24 }}>
          <div className="fef-form-grid">
            <label className="fef-label">
              Company name
              <input className="fef-input" name="companyName" required />
            </label>
            <label className="fef-label">
              Company contact
              <input className="fef-input" name="contactPerson" required />
            </label>
            <label className="fef-label">
              Company email
              <input className="fef-input" name="companyEmail" type="email" required />
            </label>
            <label className="fef-label">
              Company phone
              <div style={{ display: "flex", gap: 8 }}>
                <span
                  style={{
                    display: "grid",
                    placeItems: "center",
                    minWidth: 104,
                    border: "1px solid #dbe3ef",
                    borderRadius: 8,
                    background: "#f8fafc",
                    fontWeight: 700,
                    whiteSpace: "nowrap",
                  }}
                >
                  🇹🇿 +255
                </span>
                <input
                  className="fef-input"
                  name="companyPhone"
                  type="tel"
                  inputMode="numeric"
                  maxLength="9"
                  pattern="[67][0-9]{8}"
                  placeholder="712 345 678"
                />
              </div>
            </label>
            <label className="fef-label">
              Address
              <textarea className="fef-input" name="address" />
            </label>
            <label className="fef-label">
              TIN (optional)
              <input className="fef-input" name="tinNumber" />
            </label>
            <label className="fef-label">
              Your first name
              <input className="fef-input" name="firstName" required />
            </label>
            <label className="fef-label">
              Your last name
              <input className="fef-input" name="lastName" required />
            </label>
            <label className="fef-label">
              Username
              <input className="fef-input" name="username" minLength="3" required />
            </label>
            <label className="fef-label">
              Login email
              <input className="fef-input" name="email" type="email" required />
            </label>
            <label className="fef-label">
              Your mobile number
              <div style={{ display: "flex", gap: 8 }}>
                <span
                  style={{
                    display: "grid",
                    placeItems: "center",
                    minWidth: 104,
                    border: "1px solid #dbe3ef",
                    borderRadius: 8,
                    background: "#f8fafc",
                    fontWeight: 700,
                    whiteSpace: "nowrap",
                  }}
                >
                  🇹🇿 +255
                </span>
                <input
                  className="fef-input"
                  name="phone"
                  type="tel"
                  inputMode="numeric"
                  maxLength="9"
                  pattern="[67][0-9]{8}"
                  placeholder="712 345 678"
                  required
                />
              </div>
            </label>
            <label className="fef-label">
              Password
              <input className="fef-input" name="password" type="password" minLength="6" required />
            </label>
            <label className="fef-label">
              Confirm password
              <input
                className="fef-input"
                name="confirmPassword"
                type="password"
                minLength="6"
                required
              />
            </label>
          </div>
          <button className="fef-btn fef-btn-primary" disabled={saving} style={{ marginTop: 20 }}>
            {saving ? "Creating account…" : "Create customer account"}
          </button>
        </form>
        <p style={{ marginTop: 16 }}>
          Already registered?{" "}
          <Link to="/login" search={{ customer: true }}>
            Customer Login
          </Link>
          . Need access to an existing company? Please contact Falcon staff.
        </p>
      </main>
    </div>
  );
}
