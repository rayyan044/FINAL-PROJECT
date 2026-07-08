import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { SiteNav } from "../components/SiteNav";
import { listCustomers } from "../services/customerService";
import { listProducts } from "../services/productService";
import { createRequest } from "../services/requestService";
import {
  FiUser,
  FiPhone,
  FiMail,
  FiMapPin,
  FiCompass,
  FiCheckCircle,
  FiAlertCircle,
  FiAlertTriangle,
  FiSend,
  FiDollarSign,
  FiArrowLeft,
  FiArrowRight,
  FiTruck,
  FiActivity,
  FiDroplet
} from "react-icons/fi";
import "../styles/forms.css";
import "../styles/customer-wizard.css";

export const Route = createFileRoute("/customer")({
  head: () => ({
    meta: [
      { title: "Stranded Driver Emergency Fuel Request — FEFTMS" },
      { name: "description", content: "Submit an emergency fuel delivery request if you are stranded." },
    ],
  }),
  component: CustomerPortal,
});

const initial = {
  driverName: "",
  driverPhone: "",
  driverEmail: "",
  locationGps: "",
  locationAddress: "",
  locationLandmark: "",
  mapPin: null,
  fuelType: "Diesel",
  quantity: "",
  vehicleType: "Car",
  emergencyLevel: "Low Urgency",
  paymentMethod: "Cash on Delivery",
  notes: ""
};

const STEPS = [
  { label: "Contact", icon: FiUser },
  { label: "Location", icon: FiMapPin },
  { label: "Fuel", icon: FiTruck },
  { label: "Urgency", icon: FiAlertTriangle },
  { label: "Review", icon: FiCheckCircle }
];

const VEHICLE_TYPES = ["Car", "Motorcycle", "Truck", "Boat"];
const EMERGENCY_LEVELS = [
  { level: "Low Urgency", desc: "No immediate threat, parked safely" },
  { level: "Medium Urgency", desc: "Low fuel, need to reach destination soon" },
  { level: "Critical", desc: "Stranded / Dangerous environment / Emergency situation", highlight: true }
];
const PAYMENT_METHODS = ["Cash on Delivery", "Mobile Money (M-Pesa, Tigo, Airtel)"];

function CustomerPortal() {
  const { user } = useAuth();
  const [step, setStep] = useState(0);
  const [form, setForm] = useState(initial);
  const [products, setProducts] = useState([]);
  const [emergencyCustomerId, setEmergencyCustomerId] = useState(null);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [gpsLoading, setGpsLoading] = useState(false);
  const [errors, setErrors] = useState({});

  // Auto-fill logged in user info
  useEffect(() => {
    if (user) {
      setForm((s) => ({
        ...s,
        driverName: user.username ? user.username.charAt(0).toUpperCase() + user.username.slice(1) : "",
        driverEmail: user.email || "",
        driverPhone: user.phone || ""
      }));
    }
  }, [user]);

  // Load products and find the default emergency customer ID
  useEffect(() => {
    // Initial load
    const loadData = () => {
      listProducts({ size: 100 })
        .then((res) => setProducts(res.content || []))
        .catch((err) => console.error("Error loading products:", err));
    };

    loadData();

    listCustomers({ size: 100 })
      .then((res) => {
        const list = res.content || [];
        const emergency = list.find((c) => c.customerCode === "EMERGENCY");
        if (emergency) {
          setEmergencyCustomerId(emergency.id);
        } else if (list.length > 0) {
          setEmergencyCustomerId(list[0].id); // Fallback to first customer
        }
      })
      .catch((err) => console.error("Error loading customer base:", err));

    // Refresh products every 30 seconds to show latest prices from Finance
    const interval = setInterval(loadData, 30000);

    // Cleanup interval on unmount
    return () => clearInterval(interval);
  }, []);

  const visibleProducts = products.filter((p) => p.unitPrice != null && p.unitPrice > 0 && p.status?.toLowerCase() !== "deleted");

  const getSelectedProduct = () => {
    return visibleProducts.find((p) => p.fuelType?.toLowerCase() === form.fuelType.toLowerCase());
  };

  const formatPrice = (value) => {
    const amount = Number(value ?? 0);
    if (!Number.isFinite(amount) || amount <= 0) {
      return "Price pending";
    }
    return `TZS ${amount.toLocaleString()} per litre`;
  };

  const calculateTotal = () => {
    const qty = parseFloat(form.quantity);
    const prod = getSelectedProduct();
    if (!qty || isNaN(qty) || !prod) return 0;
    return qty * prod.unitPrice;
  };

  const update = (k) => (e) => {
    setForm((s) => ({ ...s, [k]: e.target.value }));
    if (errors[k]) {
      setErrors((errs) => {
        const copy = { ...errs };
        delete copy[k];
        return copy;
      });
    }
  };

  const selectValue = (k, v) => () => {
    setForm((s) => ({ ...s, [k]: v }));
  };

  const handleGPSDetect = () => {
    setGpsLoading(true);
    if (!navigator.geolocation) {
      alert("Geolocation is not supported by your browser");
      setGpsLoading(false);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        setForm((s) => ({
          ...s,
          locationGps: `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
          locationAddress: s.locationAddress.trim() ? s.locationAddress : `GPS Location (${latitude.toFixed(6)}, ${longitude.toFixed(6)})`
        }));
        setGpsLoading(false);
        // Clear location address validation error if it exists
        setErrors((errs) => {
          const copy = { ...errs };
          delete copy.locationAddress;
          return copy;
        });
      },
      (error) => {
        console.error(error);
        alert("Failed to auto-detect location. Please verify browser location permissions or fill in your address manually.");
        setGpsLoading(false);
      },
      { enableHighAccuracy: true, timeout: 5000 }
    );
  };

  const handleMapClick = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    setForm((s) => ({
      ...s,
      mapPin: { x, y },
      locationAddress: s.locationAddress.trim() ? s.locationAddress : `Simulated Map Pin (X: ${x.toFixed(1)}%, Y: ${y.toFixed(1)}%)`
    }));
    // Clear location address validation error if it exists
    setErrors((errs) => {
      const copy = { ...errs };
      delete copy.locationAddress;
      return copy;
    });
  };

  const validateStep = (currentStep) => {
    const errs = {};
    if (currentStep === 0) {
      if (!form.driverName.trim()) errs.driverName = "Full Name is required";
      if (!form.driverPhone.trim()) {
        errs.driverPhone = "Phone Number is required";
      } else if (!/^\+?[0-9\s\-()]{7,20}$/.test(form.driverPhone.trim())) {
        errs.driverPhone = "Invalid phone number format";
      }
      if (form.driverEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.driverEmail.trim())) {
        errs.driverEmail = "Invalid email format";
      }
    } else if (currentStep === 1) {
      if (!form.locationAddress.trim() && !form.locationGps && !form.mapPin) {
        errs.locationAddress = "Please provide an address, auto-detect GPS, or drop a map pin";
      }
    } else if (currentStep === 2) {
      const qty = parseFloat(form.quantity);
      if (!form.quantity || isNaN(qty) || qty <= 0) {
        errs.quantity = "Please specify a positive quantity";
      }
      const selectedProduct = getSelectedProduct();
      if (!selectedProduct) {
        errs.quantity = "Selected fuel is not configured yet. Please choose another product or contact Finance.";
      }
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleNext = () => {
    if (validateStep(step)) {
      setStep((s) => s + 1);
    }
  };

  const handleBack = () => {
    setStep((s) => s - 1);
  };

  const onSubmit = async (e) => {
    if (e) e.preventDefault();
    if (!validateStep(step)) return;

    setError("");
    setLoading(true);
    setSubmitted(false);

    try {
      const selectedProduct = getSelectedProduct();
      if (!selectedProduct) {
        throw new Error(`Fuel product type "${form.fuelType}" not found in database.`);
      }

      if (!emergencyCustomerId) {
        throw new Error("No customer database found. Please contact administration.");
      }

      const orderNumber = "ORD-" + Math.floor(100000 + Math.random() * 900000);
      const payload = {
        orderNumber,
        customerId: emergencyCustomerId,
        productId: selectedProduct.id,
        quantity: parseFloat(form.quantity),
        deliveryDate: new Date().toISOString(), // Immediate dispatch
        driverName: form.driverName,
        driverPhone: form.driverPhone,
        driverEmail: form.driverEmail,
        locationGps: form.locationGps,
        locationAddress: form.locationAddress,
        locationLandmark: form.locationLandmark,
        vehicleType: form.vehicleType,
        emergencyLevel: form.emergencyLevel,
        paymentMethod: form.paymentMethod,
        notes: form.notes
      };

      await createRequest(payload);
      setSubmitted(true);
      setForm(initial);
      setStep(0);
      
      // Refresh products stock list
      listProducts({ size: 100 })
        .then((res) => setProducts(res.content || []))
        .catch(console.error);
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to submit request. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fef-page">
      <SiteNav />
      <main className="fef-wizard-container fef-fade-in">
        <div className="fef-form-head">
          <h1>Stranded Driver Fuel Request</h1>
          <p>Request immediate fuel delivery to your stranded location.</p>
        </div>

        {submitted && (
          <div className="fef-alert fef-alert-success fef-fade-in" style={{ marginBottom: 24 }}>
            <FiCheckCircle style={{ verticalAlign: "-2px", marginRight: 8 }} />
            Your emergency request has been sent! Our operations team has been notified.
          </div>
        )}

        {error && (
          <div className="fef-alert fef-alert-danger fef-fade-in" style={{ marginBottom: 24 }}>
            <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 8 }} />
            {error}
          </div>
        )}

        {/* Multi-step progress node bar */}
        <div className="fef-wizard-header">
          <div className="fef-wizard-progress-bar">
            <div
              className="fef-wizard-progress-fill"
              style={{ width: `${(step / (STEPS.length - 1)) * 100}%` }}
            />
          </div>
          {STEPS.map((s, idx) => {
            const Icon = s.icon;
            const stateClass = idx === step ? "active" : idx < step ? "completed" : "";
            return (
              <div
                key={idx}
                className={`fef-wizard-step ${stateClass}`}
                onClick={() => idx < step && setStep(idx)}
              >
                <div className="fef-wizard-step-node">
                  <Icon />
                </div>
                <span className="fef-wizard-step-label">{s.label}</span>
              </div>
            );
          })}
        </div>

        {/* Wizard Form container */}
        <div className="fef-card fef-form">
          {/* STEP 1: Personal Contact Info */}
          {step === 0 && (
            <div className="fef-fade-in">
              <h3 style={{ marginBottom: 18, fontWeight: 700 }}>Driver Information</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <div className="fef-field">
                  <label className="fef-label">Full Name</label>
                  <input
                    required
                    type="text"
                    className={`fef-input ${errors.driverName ? "fef-input-invalid" : ""}`}
                    value={form.driverName}
                    onChange={update("driverName")}
                    placeholder="Enter your name"
                  />
                  {errors.driverName && <span className="fef-field-error"><FiAlertCircle /> {errors.driverName}</span>}
                </div>
                <div className="fef-field">
                  <label className="fef-label">Phone Number (Required contact)</label>
                  <input
                    required
                    type="tel"
                    className={`fef-input ${errors.driverPhone ? "fef-input-invalid" : ""}`}
                    value={form.driverPhone}
                    onChange={update("driverPhone")}
                    placeholder="e.g. +254 712 345678"
                  />
                  {errors.driverPhone && <span className="fef-field-error"><FiAlertCircle /> {errors.driverPhone}</span>}
                </div>
                <div className="fef-field">
                  <label className="fef-label">Email Address (Optional)</label>
                  <input
                    type="email"
                    className={`fef-input ${errors.driverEmail ? "fef-input-invalid" : ""}`}
                    value={form.driverEmail}
                    onChange={update("driverEmail")}
                    placeholder="e.g. driver@email.com"
                  />
                  {errors.driverEmail && <span className="fef-field-error"><FiAlertCircle /> {errors.driverEmail}</span>}
                </div>
              </div>
            </div>
          )}

          {/* STEP 2: Location Auto-detection & Map Pin */}
          {step === 1 && (
            <div className="fef-fade-in">
              <h3 style={{ marginBottom: 18, fontWeight: 700 }}>Stranded Location</h3>
              
              <button
                type="button"
                onClick={handleGPSDetect}
                disabled={gpsLoading}
                className="fef-gps-btn"
              >
                <FiCompass className={gpsLoading ? "animate-spin" : ""} />
                {gpsLoading ? "Detecting GPS..." : "AUTO-DETECT GPS LOCATION"}
              </button>

              {form.locationGps && (
                <div className="fef-gps-indicator">
                  <FiCheckCircle /> GPS Coordinates Logged: {form.locationGps}
                </div>
              )}

              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <div className="fef-field">
                  <label className="fef-label">Manual Address or Highway Marker</label>
                  <input
                    required
                    type="text"
                    className={`fef-input ${errors.locationAddress ? "fef-input-invalid" : ""}`}
                    value={form.locationAddress}
                    onChange={update("locationAddress")}
                    placeholder="e.g. Kilometer 42, Nakuru-Nairobi Highway"
                  />
                  {errors.locationAddress && <span className="fef-field-error"><FiAlertCircle /> {errors.locationAddress}</span>}
                </div>

                <div className="fef-field">
                  <label className="fef-label">Landmark / Nearby Description</label>
                  <textarea
                    className="fef-textarea"
                    value={form.locationLandmark}
                    onChange={update("locationLandmark")}
                    placeholder="e.g. Next to shell station signpost / near the bridge"
                  />
                </div>

                <div className="fef-field">
                  <label className="fef-label">Interactive Map Pin (Optional)</label>
                  <div className="fef-map-simulator" onClick={handleMapClick}>
                    <div className="fef-map-road-h" />
                    <div className="fef-map-road-v" />
                    <div className="fef-map-landmark" style={{ top: "30px", left: "20px" }}>A104 Highway</div>
                    <div className="fef-map-landmark" style={{ bottom: "20px", right: "40px" }}>Nairobi Valley</div>
                    
                    {form.mapPin && (
                      <div
                        className="fef-map-pin"
                        style={{ left: `${form.mapPin.x}%`, top: `${form.mapPin.y}%` }}
                      >
                        <FiMapPin size={24} />
                      </div>
                    )}
                    <span className="fef-map-instruction">
                      {form.mapPin ? "Location Pin Set!" : "Tap grid to drop delivery pin"}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 3: Fuel Details & Vehicle Type */}
          {step === 2 && (
            <div className="fef-fade-in">
              <h3 style={{ marginBottom: 18, fontWeight: 700 }}>Fuel Product Details</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
                
                <div className="fef-field">
                  <label className="fef-label" style={{ marginBottom: 8 }}>Fuel Type Needed</label>
                  {visibleProducts.length === 0 ? (
                    <div className="fef-alert fef-alert-warning" style={{ marginTop: 10 }}>
                      No fuel products have been configured yet. Finance will publish pricing as soon as new products are added.
                    </div>
                  ) : (
                    <div className="fef-selection-grid">
                      {visibleProducts.map((prod) => {
                        const isSelected = form.fuelType.toLowerCase() === prod.fuelType?.toLowerCase();
                        return (
                          <div
                            key={prod.id}
                            className={`fef-selection-card ${isSelected ? "selected" : ""}`}
                            onClick={selectValue("fuelType", prod.fuelType)}
                          >
                            <span className="fef-selection-card-icon"><FiDroplet /></span>
                            <h4 className="fef-selection-card-title" style={{ fontSize: "0.95rem", fontWeight: 600 }}>
                              {prod.productName} — {formatPrice(prod.unitPrice)}
                            </h4>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                <div className="fef-field">
                  <label className="fef-label">Quantity Needed (Liters)</label>
                  <input
                    required
                    type="number"
                    min="1"
                    className={`fef-input ${errors.quantity ? "fef-input-invalid" : ""}`}
                    value={form.quantity}
                    onChange={update("quantity")}
                    placeholder="e.g. 15"
                  />
                  {errors.quantity && <span className="fef-field-error"><FiAlertCircle /> {errors.quantity}</span>}
                </div>

                {calculateTotal() > 0 && (
                  <div style={{
                    marginTop: 4,
                    marginBottom: 10,
                    padding: 16,
                    borderRadius: 8,
                    background: "rgba(255, 255, 255, 0.02)",
                    border: "1px solid rgba(255, 255, 255, 0.08)",
                    display: "flex",
                    flexDirection: "column",
                    gap: 8
                  }}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, color: "var(--feftms-text-muted)" }}>
                      <span>Fuel Product:</span>
                      <span style={{ fontWeight: 600, color: "var(--feftms-text)" }}>
                        {getSelectedProduct()?.productName}
                      </span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, color: "var(--feftms-text-muted)" }}>
                      <span>Price per Litre:</span>
                      <span style={{ fontWeight: 600, color: "var(--feftms-text)" }}>
                        {getSelectedProduct() ? formatPrice(getSelectedProduct().unitPrice) : "—"}
                      </span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, color: "var(--feftms-text-muted)" }}>
                      <span>Quantity Ordered:</span>
                      <span style={{ fontWeight: 600, color: "var(--feftms-text)" }}>
                        {form.quantity} litres
                      </span>
                    </div>
                    <hr style={{ border: 0, borderTop: "1px solid rgba(255, 255, 255, 0.08)", margin: "4px 0" }} />
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontWeight: 600, color: "var(--feftms-text)" }}>Total Amount to Pay:</span>
                      <span style={{ fontSize: 16, fontWeight: 700, color: "var(--feftms-success)" }}>
                        TZS {calculateTotal().toLocaleString()}
                      </span>
                    </div>
                  </div>
                )}

                <div className="fef-field">
                  <label className="fef-label" style={{ marginBottom: 8 }}>Vehicle Type</label>
                  <div className="fef-selection-grid">
                    {VEHICLE_TYPES.map((type) => {
                      const isSelected = form.vehicleType === type;
                      return (
                        <div
                          key={type}
                          className={`fef-selection-card ${isSelected ? "selected" : ""}`}
                          onClick={selectValue("vehicleType", type)}
                        >
                          <h4 className="fef-selection-card-title">{type}</h4>
                        </div>
                      );
                    })}
                  </div>
                </div>

              </div>
            </div>
          )}

          {/* STEP 4: Emergency Urgency Level */}
          {step === 3 && (
            <div className="fef-fade-in">
              <h3 style={{ marginBottom: 18, fontWeight: 700 }}>Emergency Urgency</h3>
              
              <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                {EMERGENCY_LEVELS.map((el) => {
                  const isSelected = form.emergencyLevel === el.level;
                  const isCritical = el.level === "Critical";
                  
                  let cardClass = "";
                  if (isCritical) cardClass = "urgency-critical";
                  else if (el.level === "Low Urgency") cardClass = "urgency-low";
                  else if (el.level === "Medium Urgency") cardClass = "urgency-medium";

                  return (
                    <div
                      key={el.level}
                      className={`fef-selection-card ${cardClass} ${isSelected ? "selected" : ""}`}
                      onClick={selectValue("emergencyLevel", el.level)}
                      style={{ 
                        flexDirection: "row", 
                        textAlign: "left", 
                        padding: "16px 20px", 
                        justifyContent: "flex-start",
                        gap: 16
                      }}
                    >
                      <span className="fef-selection-card-icon" style={{ fontSize: 28 }}>
                        {isCritical ? <FiAlertTriangle /> : <FiActivity />}
                      </span>
                      <div>
                        <h4 className="fef-selection-card-title" style={{ fontSize: 16 }}>{el.level}</h4>
                        <p style={{ margin: "4px 0 0", fontSize: 12, color: "var(--feftms-text-muted)" }}>
                          {el.desc}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* STEP 5: Payment Info & Summary Review */}
          {step === 4 && (
            <div className="fef-fade-in">
              <h3 style={{ marginBottom: 18, fontWeight: 700 }}>Review & Payment</h3>
              
              <div className="fef-field" style={{ marginBottom: 20 }}>
                <label className="fef-label" style={{ marginBottom: 8 }}>Payment Method</label>
                <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                  {PAYMENT_METHODS.map((pm) => {
                    const isSelected = form.paymentMethod === pm;
                    return (
                      <div
                        key={pm}
                        className={`fef-selection-card ${isSelected ? "selected" : ""}`}
                        onClick={selectValue("paymentMethod", pm)}
                        style={{ 
                          flexDirection: "row", 
                          padding: "12px 18px", 
                          justifyContent: "flex-start",
                          gap: 12
                        }}
                      >
                        <span className="fef-selection-card-icon"><FiDollarSign /></span>
                        <h4 className="fef-selection-card-title">{pm}</h4>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="fef-field" style={{ marginBottom: 20 }}>
                <label className="fef-label">Special Delivery Instructions</label>
                <textarea
                  className="fef-textarea"
                  value={form.notes}
                  onChange={update("notes")}
                  placeholder="e.g. Leave ignition keys in tool box / I am on the shoulder lane"
                />
              </div>

              {/* Complete Request Summary Card */}
              <div className="fef-review-card">
                <h4 className="fef-review-title">Request Summary</h4>
                <div className="fef-review-row">
                  <span className="fef-review-label">Driver Name</span>
                  <span className="fef-review-value">{form.driverName}</span>
                </div>
                <div className="fef-review-row">
                  <span className="fef-review-label">Contact Phone</span>
                  <span className="fef-review-value">{form.driverPhone}</span>
                </div>
                <div className="fef-review-row">
                  <span className="fef-review-label">Location Address</span>
                  <span className="fef-review-value">{form.locationAddress}</span>
                </div>
                {form.locationGps && (
                  <div className="fef-review-row">
                    <span className="fef-review-label">GPS coordinates</span>
                    <span className="fef-review-value">{form.locationGps}</span>
                  </div>
                )}
                <div className="fef-review-row">
                  <span className="fef-review-label">Fuel Needed</span>
                  <span className="fef-review-value">{form.quantity}L {form.fuelType}</span>
                </div>
                <div className="fef-review-row">
                  <span className="fef-review-label">Vehicle Type</span>
                  <span className="fef-review-value">{form.vehicleType}</span>
                </div>
                <div className="fef-review-row">
                  <span className="fef-review-label">Urgency Level</span>
                  <span 
                    className="fef-review-value" 
                    style={{ color: form.emergencyLevel === "Critical" ? "var(--feftms-danger)" : "inherit" }}
                  >
                    {form.emergencyLevel}
                  </span>
                </div>
                 <div className="fef-review-row">
                  <span className="fef-review-label">Total Cost</span>
                  <span className="fef-review-value" style={{ color: "var(--feftms-success)", fontSize: 16, fontWeight: 700 }}>
                    TZS {calculateTotal().toLocaleString()}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Stepper Buttons actions */}
          <div className="fef-wizard-actions">
            {step > 0 ? (
              <button
                type="button"
                className="fef-btn fef-btn-outline"
                onClick={handleBack}
                disabled={loading}
              >
                <FiArrowLeft /> Back
              </button>
            ) : (
              <div />
            )}

            {step < STEPS.length - 1 ? (
              <button
                type="button"
                className="fef-btn fef-btn-primary"
                onClick={handleNext}
              >
                Next <FiArrowRight />
              </button>
            ) : (
              <button
                type="button"
                onClick={onSubmit}
                className="fef-btn fef-btn-primary"
                disabled={loading}
                style={{ 
                  background: form.emergencyLevel === "Critical" ? "var(--feftms-danger)" : "var(--feftms-primary)",
                  boxShadow: form.emergencyLevel === "Critical" ? "0 8px 20px -8px rgba(198,40,40,0.5)" : "inherit"
                }}
              >
                {loading ? "Submitting..." : <><FiSend /> REQUEST FUEL NOW</>}
              </button>
            )}
          </div>
        </div>
      </main>

      {/* Sticky Mobile CTA Action Footer */}
      {step === STEPS.length - 1 && (
        <div className="fef-sticky-cta-footer hide-on-desktop">
          <button
            type="button"
            className="fef-btn fef-btn-outline"
            onClick={handleBack}
            disabled={loading}
            style={{ flex: 1 }}
          >
            <FiArrowLeft /> Back
          </button>
          <button
            type="button"
            onClick={onSubmit}
            className="fef-btn fef-btn-primary"
            disabled={loading}
            style={{ 
              flex: 2,
              background: form.emergencyLevel === "Critical" ? "var(--feftms-danger)" : "var(--feftms-primary)",
              boxShadow: form.emergencyLevel === "Critical" ? "0 8px 20px -8px rgba(198,40,40,0.5)" : "inherit"
            }}
          >
            {loading ? "Submitting..." : "REQUEST FUEL NOW"}
          </button>
        </div>
      )}
    </div>
  );
}

