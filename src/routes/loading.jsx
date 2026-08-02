import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import {
  FiActivity,
  FiClipboard,
  FiCheckCircle,
  FiDroplet,
  FiHome,
  FiArrowRight,
  FiLock,
  FiCheck,
  FiPrinter,
  FiTrash2,
  FiPlus,
  FiAlertTriangle,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { Link } from "@tanstack/react-router";
import {
  listLoadingOrders,
  startLoadingActivity,
  completeLoadingActivity,
  getLoadingReport,
} from "../services/loadingOrderService";
import { listProducts } from "../services/productService";
import "../styles/forms.css";
import falconLogo from "../assets/falcon-logo.png";

export const Route = createFileRoute("/loading")({
  head: () => ({ meta: [{ title: "Loading Officer — FEFTMS" }] }),
  component: LoadingDash,
});

const SIDE = [
  { key: "dash", label: "Loading Officer", icon: FiActivity },
];

function LoadingDash() {
  const [loadingOrders, setLoadingOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Selection states
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedActivity, setSelectedActivity] = useState(null);
  const [activeStep, setActiveStep] = useState(1); // 1: Start, 2: Compartments, 3: Meter & Remarks, 4: Review, 5: Report

  // Wizard forms
  const [startForm, setStartForm] = useState({ bayNumber: "BAY-1", pumpNumber: "" });
  const [compartments, setCompartments] = useState([
    { compartmentNumber: 1, capacity: "10000", ambientVolume: "", temperature: "25", density: "0.840", sealNumber: "" },
  ]);
  const [meterForm, setMeterForm] = useState({ meterStart: "", meterEnd: "", remarks: "" });
  const [reportData, setReportData] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const ordersRes = await listLoadingOrders();
      setLoadingOrders(ordersRes.data || ordersRes);
      
      const prodRes = await listProducts();
      setProducts(prodRes.data || prodRes);
    } catch (err) {
      setError("Failed to load loading queue data.");
    } finally {
      setLoading(false);
    }
  };

  const getProductCoefficient = (prodIdOrName) => {
    // Attempt to match expansion coefficient from product list, otherwise fallback
    const matched = products.find(
      (p) => p.id === prodIdOrName || p.productName.toLowerCase() === String(prodIdOrName).toLowerCase()
    );
    if (matched && matched.thermalExpansionCoefficient) {
      return matched.thermalExpansionCoefficient;
    }
    // Standard defaults
    const nameStr = String(prodIdOrName).toLowerCase();
    if (nameStr.includes("pms") || nameStr.includes("petrol") || nameStr.includes("gasoline")) {
      return 0.00120;
    }
    return 0.00084; // AGO/Diesel
  };

  const calculateStandardVolume = (ambient, temp, coeff) => {
    const ambVal = parseFloat(ambient) || 0;
    const tempVal = parseFloat(temp) || 20;
    const alpha = parseFloat(coeff) || 0.00084;
    return ambVal * (1 - alpha * (tempVal - 20));
  };

  const handleStartLoadingClick = async () => {
    setError("");
    setSuccess("");
    try {
      const res = await startLoadingActivity(selectedOrder.id, selectedActivity.id, {
        bayNumber: startForm.bayNumber,
        pumpNumber: startForm.pumpNumber,
      });
      setSuccess("Loading activity started successfully.");
      
      // Update local state
      const updatedOrder = res.data || res;
      setSelectedOrder(updatedOrder);
      const updatedAct = updatedOrder.activities.find((a) => a.id === selectedActivity.id);
      setSelectedActivity(updatedAct);
      
      // Move to compartments
      setActiveStep(2);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to start loading.");
    }
  };

  const handleAddCompartment = () => {
    setCompartments((prev) => [
      ...prev,
      {
        compartmentNumber: prev.length + 1,
        capacity: "10000",
        ambientVolume: "",
        temperature: "25",
        density: "0.840",
        sealNumber: "",
      },
    ]);
  };

  const handleRemoveCompartment = (index) => {
    if (compartments.length === 1) return;
    const updated = compartments.filter((_, i) => i !== index).map((c, i) => ({
      ...c,
      compartmentNumber: i + 1,
    }));
    setCompartments(updated);
  };

  const handleCompartmentChange = (index, field, value) => {
    setCompartments((prev) => {
      const updated = [...prev];
      updated[index][field] = value;
      return updated;
    });
  };

  const handlePrintReport = () => {
    const printContent = document.getElementById("printable-loading-report");
    if (!printContent || !reportData) return;

    const win = window.open("", "_blank");
    win.document.write(`
      <html>
        <head>
          <title>Loading Report - ${reportData.reportNumber}</title>
          <style>
            body { font-family: 'Outfit', sans-serif; padding: 40px; color: #1e293b; background: #fff; }
            .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #e2e8f0; padding-bottom: 20px; margin-bottom: 30px; }
            .logo { height: 50px; }
            .title { font-size: 24px; font-weight: bold; color: #0f172a; margin: 0; }
            .meta-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 30px; }
            .meta-item { font-size: 14px; color: #475569; }
            .meta-item strong { color: #0f172a; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
            th, td { padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: left; }
            th { background-color: #f8fafc; font-weight: 600; color: #0f172a; }
            .summary { display: flex; justify-content: flex-end; gap: 40px; font-size: 16px; font-weight: bold; margin-bottom: 40px; }
            .signatures { display: flex; justify-content: space-between; margin-top: 60px; border-top: 1px solid #e2e8f0; padding-top: 20px; }
            .sig-line { width: 200px; text-align: center; }
            .sig-line div { border-top: 1px solid #475569; margin-top: 40px; font-size: 12px; color: #64748b; }
            @media print {
              body { padding: 0; }
              @page { margin: 20mm; }
            }
          </style>
        </head>
        <body onload="window.print(); window.close();">
          ${printContent.innerHTML}
        </body>
      </html>
    `);
    win.document.close();
  };

  const handleCompleteSubmit = async () => {
    setError("");
    setSuccess("");
    try {
      // Find matches product ID
      const matchedProd = products.find(
        (p) => p.productName.toLowerCase() === selectedActivity.product.toLowerCase()
      );
      if (!matchedProd) {
        throw new Error("Unable to link loading activity product to inventory ID.");
      }

      const payload = {
        bayNumber: startForm.bayNumber,
        pumpNumber: startForm.pumpNumber,
        meterStart: parseFloat(meterForm.meterStart) || 0,
        meterEnd: parseFloat(meterForm.meterEnd) || 0,
        remarks: meterForm.remarks,
        compartments: compartments.map((c) => ({
          compartmentNumber: parseInt(c.compartmentNumber),
          capacity: parseFloat(c.capacity) || 0,
          productId: matchedProd.id,
          ambientVolume: parseFloat(c.ambientVolume) || 0,
          temperature: parseFloat(c.temperature) || 0,
          density: parseFloat(c.density) || 0,
          sealNumber: c.sealNumber,
        })),
      };

      const res = await completeLoadingActivity(selectedOrder.id, selectedActivity.id, payload);
      setSuccess("Loading completed. stock deducted and report generated.");
      
      // Fetch the generated loading report
      const repRes = await getLoadingReport(selectedActivity.id);
      setReportData(repRes.data || repRes);
      
      // Move to report view step
      setActiveStep(5);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to complete loading activities.");
    }
  };

  const activeActivities = loadingOrders.flatMap((o) =>
    (o.activities || []).map((a) => ({ ...a, order: o }))
  );

  const pendingCount = activeActivities.filter((a) => a.status === "PENDING").length;
  const loadingCount = activeActivities.filter((a) => a.status === "STARTED").length;
  const completedCount = activeActivities.filter((a) => a.status === "COMPLETED").length;

  return (
    <DashboardLayout role="OPERATOR" sideItems={SIDE} activeKey="dash">
      <PageHeader title="Loading Operations (Bays)" crumbs={["Loading", "Queue"]} />

      <div style={{ padding: 24, maxWidth: 1400, margin: "0 auto" }}>
        {/* Statistics Cards */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 24, marginBottom: 30 }}>
          <StatCard title="Bays Queue" value={pendingCount} color="var(--primary-color)" subtitle="Orders Approved for Loading" />
          <StatCard title="Loading In Progress" value={loadingCount} color="var(--accent-amber)" subtitle="Trucks Active in Bays" />
          <StatCard title="Completed Today" value={completedCount} color="var(--accent-emerald)" subtitle="Fully Loaded Trucks" />
        </div>

        {error && (
          <div className="fef-alert fef-alert-error" style={{ marginBottom: 24 }}>
            <FiAlertTriangle /> {error}
          </div>
        )}
        {success && (
          <div className="fef-alert fef-alert-success" style={{ marginBottom: 24 }}>
            <FiCheck /> {success}
          </div>
        )}

        <div style={{ display: "grid", gridTemplateColumns: selectedActivity ? "1fr" : "380px 1fr", gap: 24 }}>
          
          {/* Main Wizard Area (visible when activity is selected) */}
          {selectedActivity ? (
            <div className="fef-card" style={{ padding: 32 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--border-color)", paddingBottom: 20, marginBottom: 24 }}>
                <div>
                  <h2 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>
                    Loading Activity Wizard: Truck {selectedActivity.truckNumber}
                  </h2>
                  <span style={{ fontSize: 13, color: "var(--text-muted)" }}>
                    Order: {selectedActivity.order.loadingOrderNumber} &bull; Product: {selectedActivity.product}
                  </span>
                </div>
                <button
                  className="fef-btn fef-btn-outline"
                  onClick={() => {
                    setSelectedActivity(null);
                    setSelectedOrder(null);
                    setActiveStep(1);
                    setReportData(null);
                  }}
                >
                  Back to Queue
                </button>
              </div>

              {/* Progress Tracker */}
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 40, position: "relative" }}>
                <div style={{ position: "absolute", top: 15, left: "5%", right: "5%", height: 2, background: "var(--border-color)", zIndex: 0 }}></div>
                {[
                  { step: 1, label: "Bay Allocation" },
                  { step: 2, label: "Compartment Entry" },
                  { step: 3, label: "Meter & Remarks" },
                  { step: 4, label: "Review & Submit" },
                  { step: 5, label: "Loading Report" },
                ].map((s) => (
                  <div key={s.step} style={{ zIndex: 1, display: "flex", flexDirection: "column", alignItems: "center", width: "15%" }}>
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: "50%",
                        background: activeStep >= s.step ? "var(--primary-color)" : "var(--bg-light)",
                        color: activeStep >= s.step ? "#fff" : "var(--text-muted)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontWeight: "bold",
                        fontSize: 14,
                        border: activeStep >= s.step ? "none" : "1px solid var(--border-color)",
                      }}
                    >
                      {activeStep > s.step ? <FiCheck /> : s.step}
                    </div>
                    <span style={{ fontSize: 12, marginTop: 8, textAlign: "center", fontWeight: activeStep === s.step ? 600 : 400, color: activeStep === s.step ? "var(--text-dark)" : "var(--text-muted)" }}>
                      {s.label}
                    </span>
                  </div>
                ))}
              </div>

              {/* Step 1: Start Loading / Bay Allocation */}
              {activeStep === 1 && (
                <div style={{ maxWidth: 500, margin: "0 auto" }}>
                  <h3 style={{ fontSize: 18, marginBottom: 20 }}>Allocate Bay and Start Loading</h3>
                  <div className="fef-form-group">
                    <label className="fef-label">Loading Bay</label>
                    <select
                      className="fef-input"
                      value={startForm.bayNumber}
                      onChange={(e) => setStartForm({ ...startForm, bayNumber: e.target.value })}
                    >
                      <option value="BAY-1">Loading Bay 1</option>
                      <option value="BAY-2">Loading Bay 2</option>
                      <option value="BAY-3">Loading Bay 3</option>
                    </select>
                  </div>
                  <div className="fef-form-group">
                    <label className="fef-label">Pump Number / Dispenser</label>
                    <input
                      type="text"
                      className="fef-input"
                      placeholder="e.g., PUMP-04"
                      value={startForm.pumpNumber}
                      onChange={(e) => setStartForm({ ...startForm, pumpNumber: e.target.value })}
                    />
                  </div>
                  <button
                    className="fef-btn fef-btn-primary"
                    style={{ width: "100%", marginTop: 20 }}
                    onClick={handleStartLoadingClick}
                  >
                    Start Loading Process <FiArrowRight style={{ marginLeft: 8 }} />
                  </button>
                </div>
              )}

              {/* Step 2: Compartment Entry */}
              {activeStep === 2 && (
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
                    <h3 style={{ fontSize: 18, margin: 0 }}>Compartment Physical Measurements</h3>
                    <button className="fef-btn fef-btn-outline" onClick={handleAddCompartment}>
                      <FiPlus /> Add Compartment
                    </button>
                  </div>

                  <table className="fef-table">
                    <thead>
                      <tr>
                        <th>No.</th>
                        <th>Capacity (L)</th>
                        <th>Ambient Volume (L)</th>
                        <th>Temp (°C)</th>
                        <th>Density (kg/L)</th>
                        <th>Seal Number</th>
                        <th>Est. Std Volume (20°C)</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {compartments.map((comp, idx) => {
                        const coeff = getProductCoefficient(selectedActivity.product);
                        const estStd = calculateStandardVolume(comp.ambientVolume, comp.temperature, coeff);
                        return (
                          <tr key={idx}>
                            <td>{comp.compartmentNumber}</td>
                            <td>
                              <input
                                type="number"
                                className="fef-input"
                                style={{ width: 100 }}
                                value={comp.capacity}
                                onChange={(e) => handleCompartmentChange(idx, "capacity", e.target.value)}
                              />
                            </td>
                            <td>
                              <input
                                type="number"
                                className="fef-input"
                                style={{ width: 120 }}
                                placeholder="Ambient L"
                                value={comp.ambientVolume}
                                onChange={(e) => handleCompartmentChange(idx, "ambientVolume", e.target.value)}
                              />
                            </td>
                            <td>
                              <input
                                type="number"
                                className="fef-input"
                                style={{ width: 80 }}
                                value={comp.temperature}
                                onChange={(e) => handleCompartmentChange(idx, "temperature", e.target.value)}
                              />
                            </td>
                            <td>
                              <input
                                type="number"
                                step="0.001"
                                className="fef-input"
                                style={{ width: 90 }}
                                value={comp.density}
                                onChange={(e) => handleCompartmentChange(idx, "density", e.target.value)}
                              />
                            </td>
                            <td>
                              <input
                                type="text"
                                className="fef-input"
                                placeholder="Seal #"
                                value={comp.sealNumber}
                                onChange={(e) => handleCompartmentChange(idx, "sealNumber", e.target.value)}
                              />
                            </td>
                            <td style={{ fontWeight: 600 }}>
                              {estStd.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} L
                            </td>
                            <td>
                              <button
                                className="fef-btn fef-btn-outline"
                                style={{ color: "var(--accent-red)", padding: 8 }}
                                onClick={() => handleRemoveCompartment(idx)}
                              >
                                <FiTrash2 />
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>

                  <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 24 }}>
                    <button className="fef-btn fef-btn-primary" onClick={() => setActiveStep(3)}>
                      Proceed to Meters <FiArrowRight style={{ marginLeft: 8 }} />
                    </button>
                  </div>
                </div>
              )}

              {/* Step 3: Meter Readings & Remarks */}
              {activeStep === 3 && (
                <div style={{ maxWidth: 500, margin: "0 auto" }}>
                  <h3 style={{ fontSize: 18, marginBottom: 20 }}>Record Terminal Meter Readings</h3>
                  <div className="fef-form-group">
                    <label className="fef-label">Meter Start Reading (L)</label>
                    <input
                      type="number"
                      className="fef-input"
                      value={meterForm.meterStart}
                      onChange={(e) => setMeterForm({ ...meterForm, meterStart: e.target.value })}
                    />
                  </div>
                  <div className="fef-form-group">
                    <label className="fef-label">Meter End Reading (L)</label>
                    <input
                      type="number"
                      className="fef-input"
                      value={meterForm.meterEnd}
                      onChange={(e) => setMeterForm({ ...meterForm, meterEnd: e.target.value })}
                    />
                  </div>
                  <div className="fef-form-group">
                    <label className="fef-label">Remarks / Operational Notes</label>
                    <textarea
                      className="fef-input"
                      rows={3}
                      value={meterForm.remarks}
                      onChange={(e) => setMeterForm({ ...meterForm, remarks: e.target.value })}
                    />
                  </div>
                  <div style={{ display: "flex", gap: 12, marginTop: 24 }}>
                    <button className="fef-btn fef-btn-outline" style={{ width: "50%" }} onClick={() => setActiveStep(2)}>
                      Back
                    </button>
                    <button className="fef-btn fef-btn-primary" style={{ width: "50%" }} onClick={() => setActiveStep(4)}>
                      Review Loading Details <FiArrowRight style={{ marginLeft: 8 }} />
                    </button>
                  </div>
                </div>
              )}

              {/* Step 4: Review and Submit */}
              {activeStep === 4 && (
                <div>
                  <h3 style={{ fontSize: 18, marginBottom: 20 }}>Verify Observations & Calculate Standard Volumes</h3>

                  <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: 24, marginBottom: 30 }}>
                    <div style={{ background: "var(--bg-light)", padding: 20, borderRadius: 8 }}>
                      <h4 style={{ margin: "0 0 12px 0", fontSize: 14, textTransform: "uppercase", color: "var(--text-muted)" }}>
                        Meters & Settings
                      </h4>
                      <p><strong>Loading Bay:</strong> {startForm.bayNumber}</p>
                      <p><strong>Pump/Dispenser:</strong> {startForm.pumpNumber || "N/A"}</p>
                      <p><strong>Meter Start:</strong> {parseFloat(meterForm.meterStart).toLocaleString()} L</p>
                      <p><strong>Meter End:</strong> {parseFloat(meterForm.meterEnd).toLocaleString()} L</p>
                      <p><strong>Meter Difference:</strong> {(parseFloat(meterForm.meterEnd) - parseFloat(meterForm.meterStart)).toLocaleString()} L</p>
                    </div>
                    <div style={{ background: "var(--bg-light)", padding: 20, borderRadius: 8 }}>
                      <h4 style={{ margin: "0 0 12px 0", fontSize: 14, textTransform: "uppercase", color: "var(--text-muted)" }}>
                        Order Summary
                      </h4>
                      <p><strong>Allocated Target:</strong> {selectedActivity.allocatedQuantity.toLocaleString()} L</p>
                      <p><strong>Product:</strong> {selectedActivity.product}</p>
                      <p>
                        <strong>Total Ambient Loaded:</strong>{" "}
                        {compartments.reduce((acc, c) => acc + (parseFloat(c.ambientVolume) || 0), 0).toLocaleString()} L
                      </p>
                      <p>
                        <strong>Total Standard (20°C) Preview:</strong>{" "}
                        {compartments.reduce((acc, c) => {
                          const coeff = getProductCoefficient(selectedActivity.product);
                          return acc + calculateStandardVolume(c.ambientVolume, c.temperature, coeff);
                        }, 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} L
                      </p>
                    </div>
                  </div>

                  <table className="fef-table" style={{ marginBottom: 30 }}>
                    <thead>
                      <tr>
                        <th>Compartment</th>
                        <th>Capacity (L)</th>
                        <th>Observed Ambient (L)</th>
                        <th>Temp (°C)</th>
                        <th>Observed Density</th>
                        <th>Seals</th>
                        <th>Std Volume (20°C)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {compartments.map((comp, idx) => {
                        const coeff = getProductCoefficient(selectedActivity.product);
                        const estStd = calculateStandardVolume(comp.ambientVolume, comp.temperature, coeff);
                        return (
                          <tr key={idx}>
                            <td>Compartment {comp.compartmentNumber}</td>
                            <td>{parseFloat(comp.capacity).toLocaleString()} L</td>
                            <td>{parseFloat(comp.ambientVolume).toLocaleString()} L</td>
                            <td>{comp.temperature} °C</td>
                            <td>{comp.density}</td>
                            <td>{comp.sealNumber || "N/A"}</td>
                            <td>{estStd.toLocaleString()} L</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>

                  <div style={{ display: "flex", gap: 12, justifyContent: "flex-end" }}>
                    <button className="fef-btn fef-btn-outline" onClick={() => setActiveStep(3)}>
                      Back
                    </button>
                    <button className="fef-btn fef-btn-primary" onClick={handleCompleteSubmit}>
                      Complete & Deduct Inventory <FiCheckCircle style={{ marginLeft: 8 }} />
                    </button>
                  </div>
                </div>
              )}

              {/* Step 5: Loading Report Display */}
              {activeStep === 5 && reportData && (
                <div>
                  <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 20 }}>
                    <button className="fef-btn fef-btn-primary" onClick={handlePrintReport}>
                      <FiPrinter style={{ marginRight: 8 }} /> Print Loading Report
                    </button>
                  </div>

                  {/* Printable layout structure */}
                  <div
                    id="printable-loading-report"
                    style={{
                      border: "1px solid var(--border-color)",
                      padding: 40,
                      borderRadius: 8,
                      background: "#fff",
                      color: "#1e293b",
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "2px solid #e2e8f0", paddingBottom: 20, marginBottom: 30 }}>
                      <div>
                        <img src={falconLogo} alt="Falcon Energy" style={{ height: 45, marginBottom: 12 }} />
                        <h1 style={{ fontSize: 22, fontWeight: "bold", margin: 0 }}>LOADING OPERATIONAL REPORT</h1>
                      </div>
                      <div style={{ textAlign: "right" }}>
                        <p style={{ margin: "0 0 4px 0", fontSize: 13 }}><strong>Report No:</strong> {reportData.reportNumber}</p>
                        <p style={{ margin: 0, fontSize: 13 }}><strong>Date:</strong> {new Date(reportData.createdAt).toLocaleString()}</p>
                      </div>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: 24, marginBottom: 30 }}>
                      <div>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Customer Name:</strong> {selectedOrder.customerName}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Invoice:</strong> {selectedOrder.invoiceNumber || "—"}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Loading Order:</strong> {selectedOrder.loadingOrderNumber}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Terminal / Depot:</strong> {reportData.terminal}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Loading Bay:</strong> {reportData.loadingBay}</p>
                      </div>
                      <div>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Truck Number:</strong> {selectedActivity.truckNumber}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Trailer Number:</strong> {selectedActivity.trailerNumber || "N/A"}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Driver:</strong> {selectedActivity.driverName}</p>
                        <p style={{ margin: "0 0 8px 0" }}><strong>Officer:</strong> {reportData.loadingOfficer}</p>
                      </div>
                    </div>

                    <table style={{ width: "100%", borderCollapse: "collapse", marginBottom: 30 }}>
                      <thead>
                        <tr style={{ borderBottom: "2px solid #cbd5e1", textAlign: "left", fontSize: 13, color: "#475569" }}>
                          <th style={{ padding: "8px 0" }}>Compartment</th>
                          <th>Product</th>
                          <th>Capacity</th>
                          <th>Observed Volume</th>
                          <th>Temp (°C)</th>
                          <th>Density</th>
                          <th style={{ textAlign: "right" }}>Std Volume (20°C)</th>
                        </tr>
                      </thead>
                      <tbody>
                        {compartments.map((comp, index) => {
                          const coeff = getProductCoefficient(selectedActivity.product);
                          const std = calculateStandardVolume(comp.ambientVolume, comp.temperature, coeff);
                          return (
                            <tr key={index} style={{ borderBottom: "1px solid #e2e8f0", fontSize: 13 }}>
                              <td style={{ padding: "12px 0" }}>Compartment {comp.compartmentNumber}</td>
                              <td>{selectedActivity.product}</td>
                              <td>{parseFloat(comp.capacity).toLocaleString()} L</td>
                              <td>{parseFloat(comp.ambientVolume).toLocaleString()} L</td>
                              <td>{comp.temperature} °C</td>
                              <td>{comp.density}</td>
                              <td style={{ textAlign: "right", fontWeight: "bold" }}>{std.toLocaleString()} L</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>

                    <div style={{ display: "flex", justifyContent: "flex-end", gap: 40, borderTop: "2px solid #cbd5e1", paddingTop: 15 }}>
                      <p><strong>Total Ambient Volume:</strong> {compartments.reduce((acc, c) => acc + (parseFloat(c.ambientVolume) || 0), 0).toLocaleString()} L</p>
                      <p><strong>Total Standard Volume @ 20°C:</strong> {compartments.reduce((acc, c) => {
                        const coeff = getProductCoefficient(selectedActivity.product);
                        return acc + calculateStandardVolume(c.ambientVolume, c.temperature, coeff);
                      }, 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} L</p>
                    </div>

                    <div style={{ marginTop: 50, display: "flex", justifyContent: "space-between" }}>
                      <div style={{ borderTop: "1px solid #94a3b8", width: 220, textAlign: "center", paddingTop: 8 }}>
                        <span style={{ fontSize: 11, color: "#64748b" }}>Loading Officer Signature</span>
                      </div>
                      <div style={{ borderTop: "1px solid #94a3b8", width: 220, textAlign: "center", paddingTop: 8 }}>
                        <span style={{ fontSize: 11, color: "#64748b" }}>Driver / Transporter Signature</span>
                      </div>
                    </div>
                  </div>

                  <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 24 }}>
                    <button
                      className="fef-btn fef-btn-outline"
                      onClick={() => {
                        setSelectedActivity(null);
                        setSelectedOrder(null);
                        setActiveStep(1);
                        setReportData(null);
                      }}
                    >
                      Back to Dashboard
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <>
              {/* Sidebar Left: Summary Queue */}
              <div className="fef-card" style={{ padding: 20 }}>
                <h3 style={{ margin: "0 0 16px 0", fontSize: 16, fontWeight: 700 }}>Bays & Queues</h3>
                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  {activeActivities.length === 0 ? (
                    <p style={{ color: "var(--text-muted)", fontSize: 13 }}>No active trucks in queue today.</p>
                  ) : (
                    activeActivities.map((act) => {
                      let statusBg = "var(--border-color)";
                      let statusText = "var(--text-muted)";
                      if (act.status === "PENDING") {
                        statusBg = "rgba(79, 70, 229, 0.1)";
                        statusText = "var(--primary-color)";
                      } else if (act.status === "STARTED") {
                        statusBg = "rgba(245, 158, 11, 0.1)";
                        statusText = "var(--accent-amber)";
                      } else if (act.status === "COMPLETED") {
                        statusBg = "rgba(16, 185, 129, 0.1)";
                        statusText = "var(--accent-emerald)";
                      }

                      return (
                        <div
                          key={act.id}
                          style={{
                            padding: 14,
                            borderRadius: 8,
                            border: "1px solid var(--border-color)",
                            background: "var(--bg-light)",
                            cursor: "pointer",
                          }}
                          onClick={() => {
                            setSelectedActivity(act);
                            setSelectedOrder(act.order);
                            if (act.status === "STARTED") setActiveStep(2);
                            else if (act.status === "COMPLETED") {
                              // If completed, fetch report directly
                              getLoadingReport(act.id).then((r) => {
                                setReportData(r.data || r);
                                setActiveStep(5);
                              });
                            } else {
                              setActiveStep(1);
                            }
                          }}
                        >
                          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                            <span style={{ fontWeight: 600, fontSize: 14 }}>{act.truckNumber}</span>
                            <span
                              style={{
                                fontSize: 10,
                                fontWeight: "bold",
                                padding: "2px 6px",
                                borderRadius: 4,
                                backgroundColor: statusBg,
                                color: statusText,
                              }}
                            >
                              {act.status}
                            </span>
                          </div>
                          <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                            {act.product} &bull; {act.allocatedQuantity.toLocaleString()} L
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>

              {/* Center Content: Queue Detail List */}
              <div className="fef-card" style={{ padding: 24 }}>
                <h3 style={{ margin: "0 0 20px 0", fontSize: 18, fontWeight: 700 }}>Approved Loading Orders</h3>
                
                {loading ? (
                  <p>Loading queue...</p>
                ) : loadingOrders.length === 0 ? (
                  <p style={{ color: "var(--text-muted)" }}>No approved loading orders found.</p>
                ) : (
                  <table className="fef-table">
                    <thead>
                      <tr>
                        <th>Order Number</th>
                        <th>Terminal</th>
                        <th>Consignee</th>
                        <th>Allocated (L)</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {loadingOrders.map((lo) => (
                        <tr key={lo.id}>
                          <td><strong>{lo.loadingOrderNumber}</strong></td>
                          <td>{lo.loadingTerminal}</td>
                          <td>{lo.consignee}</td>
                          <td>
                            {lo.activities.reduce((acc, act) => acc + act.allocatedQuantity, 0).toLocaleString()} L
                          </td>
                          <td>
                            <span className={`fef-badge fef-badge-${lo.status.toLowerCase()}`}>
                              {lo.status}
                            </span>
                          </td>
                          <td>
                            <button
                              className="fef-btn fef-btn-primary fef-btn-sm"
                              onClick={() => {
                                // Select first pending activity for this order
                                const firstPending = lo.activities.find((a) => a.status === "PENDING" || a.status === "STARTED") || lo.activities[0];
                                setSelectedActivity({ ...firstPending, order: lo });
                                setSelectedOrder(lo);
                                if (firstPending.status === "STARTED") setActiveStep(2);
                                else if (firstPending.status === "COMPLETED") {
                                  getLoadingReport(firstPending.id).then((r) => {
                                    setReportData(r.data || r);
                                    setActiveStep(5);
                                  });
                                } else {
                                  setActiveStep(1);
                                }
                              }}
                            >
                              Manage Bays
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </>
          )}

        </div>
      </div>
    </DashboardLayout>
  );
}
