import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { FiEye, FiEyeOff } from "react-icons/fi";
import { registerCustomer } from "../services/authService";
import { SiteNav } from "../components/SiteNav";
import "../styles/forms.css";

export const Route = createFileRoute("/customer-register")({ component: CustomerRegister });
const COUNTRY_CODES = [
  { code: "+255", label: "🇹🇿 Tanzania (+255)" },
  { code: "+254", label: "🇰🇪 Kenya (+254)" },
  { code: "+256", label: "🇺🇬 Uganda (+256)" },
  { code: "+250", label: "🇷🇼 Rwanda (+250)" },
  { code: "+260", label: "🇿🇲 Zambia (+260)" },
  { code: "+27", label: "🇿🇦 South Africa (+27)" },
  { code: "+44", label: "🇬🇧 United Kingdom (+44)" },
  { code: "+1", label: "🇺🇸 United States / Canada (+1)" },
];

const digitsOnly = (value) => String(value || "").replace(/\D/g, "");
const toInternationalPhone = (countryCode, value) => {
  const nationalNumber = digitsOnly(value).replace(/^0+/, "");
  return nationalNumber ? `${countryCode}${nationalNumber}` : "";
};
const isValidInternationalPhone = (value) => /^\+[1-9]\d{7,14}$/.test(value);

function CustomerRegister() {
  const navigate = useNavigate(),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false),
    [companyCountryCode, setCompanyCountryCode] = useState("+255"),
    [phoneCountryCode, setPhoneCountryCode] = useState("+255"),
    [showPassword, setShowPassword] = useState(false),
    [showConfirmPassword, setShowConfirmPassword] = useState(false),
    [phoneErrors, setPhoneErrors] = useState({});

  const validatePhone = (name, countryCode, value, required) => {
    const internationalPhone = toInternationalPhone(countryCode, value);
    const message = !value && required
      ? "Enter a phone number."
      : value && !isValidInternationalPhone(internationalPhone)
        ? "Enter a valid phone number (8–15 digits including the country code)."
        : "";
    setPhoneErrors((current) => ({ ...current, [name]: message }));
    return { internationalPhone, message };
  };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    const x = Object.fromEntries(new FormData(e.currentTarget));
    const companyPhone = validatePhone("companyPhone", companyCountryCode, x.companyPhone, false);
    const phone = validatePhone("phone", phoneCountryCode, x.phone, true);
    if (companyPhone.message || phone.message) {
      setError("Please correct the highlighted phone number.");
      setSaving(false);
      return;
    }
    if (companyPhone.internationalPhone) x.companyPhone = companyPhone.internationalPhone;
    else delete x.companyPhone;
    x.phone = phone.internationalPhone;
    if (x.password !== x.confirmPassword) {
      setError("Passwords do not match.");
      setSaving(false);
      return;
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
                <select className="fef-input" value={companyCountryCode} onChange={(e) => setCompanyCountryCode(e.target.value)} style={{ maxWidth: 190 }} aria-label="Company phone country code">
                  {COUNTRY_CODES.map((country) => <option key={country.code} value={country.code}>{country.label}</option>)}
                </select>
                <input
                  className={`fef-input ${phoneErrors.companyPhone ? "fef-input-invalid" : ""}`}
                  name="companyPhone"
                  type="tel"
                  inputMode="numeric"
                  maxLength="15"
                  placeholder="712 345 678"
                  aria-invalid={Boolean(phoneErrors.companyPhone)}
                  onBlur={(e) => validatePhone("companyPhone", companyCountryCode, e.target.value, false)}
                />
              </div>
              {phoneErrors.companyPhone && <small className="fef-field-error">{phoneErrors.companyPhone}</small>}
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
                <select className="fef-input" value={phoneCountryCode} onChange={(e) => setPhoneCountryCode(e.target.value)} style={{ maxWidth: 190 }} aria-label="Mobile phone country code">
                  {COUNTRY_CODES.map((country) => <option key={country.code} value={country.code}>{country.label}</option>)}
                </select>
                <input
                  className={`fef-input ${phoneErrors.phone ? "fef-input-invalid" : ""}`}
                  name="phone"
                  type="tel"
                  inputMode="numeric"
                  maxLength="15"
                  placeholder="712 345 678"
                  required
                  aria-invalid={Boolean(phoneErrors.phone)}
                  onBlur={(e) => validatePhone("phone", phoneCountryCode, e.target.value, true)}
                />
              </div>
              {phoneErrors.phone && <small className="fef-field-error">{phoneErrors.phone}</small>}
            </label>
            <label className="fef-label">
              Password
              <div className="fef-password-wrapper">
                <input className="fef-input" name="password" type={showPassword ? "text" : "password"} minLength="6" required />
                <button type="button" className="fef-password-toggle" onClick={() => setShowPassword((visible) => !visible)} aria-label={showPassword ? "Hide password" : "Show password"}>
                  {showPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </button>
              </div>
            </label>
            <label className="fef-label">
              Confirm password
              <div className="fef-password-wrapper">
                <input className="fef-input" name="confirmPassword" type={showConfirmPassword ? "text" : "password"} minLength="6" required />
                <button type="button" className="fef-password-toggle" onClick={() => setShowConfirmPassword((visible) => !visible)} aria-label={showConfirmPassword ? "Hide confirm password" : "Show confirm password"}>
                  {showConfirmPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </button>
              </div>
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
