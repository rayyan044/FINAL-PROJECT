import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import {
  FiHome,
  FiTruck,
  FiMapPin,
  FiCheckCircle,
  FiAlertCircle,
  FiX,
  FiClipboard,
  FiInfo,
  FiArrowRight,
  FiFileText,
  FiClock,
  FiFlag,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { OperatorWorkflowProgress } from "../components/OperatorWorkflowProgress";
import {
  getActiveDeliveries,
  getDeliveryHistory,
  recordArrival,
  completeDelivery,
} from "../services/deliveryService";

export const Route = createFileRoute("/deliveries")({
  head: () => ({ meta: [{ title: "Delivery Workspace — FEFTMS" }] }),
  component: DeliveryDash,
});

const SIDE = [
  { key: "operations", label: "Operations", icon: FiHome },
  { key: "documents", label: "Delivery Documents", icon: FiFileText },
  { key: "dispatch", label: "Dispatch Management", icon: FiArrowRight },
  { key: "dash", label: "Delivery Management", icon: FiHome },
  { key: "active", label: "Active Transits", icon: FiTruck },
  { key: "history", label: "Delivery History", icon: FiCheckCircle },
];

function DeliveryDash() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("dash");
  const [activeDeliveries, setActiveDeliveries] = useState([]);
  const [historyDeliveries, setHistoryDeliveries] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modals
  const [selectedDeliveryDetails, setSelectedDeliveryDetails] = useState(null);
  const [arrivalModalDelivery, setArrivalModalDelivery] = useState(null);
  const [completeModalDelivery, setCompleteModalDelivery] = useState(null);

  // Forms
  const [receivedBy, setReceivedBy] = useState("");
  const [completedBy, setCompletedBy] = useState("");
  const [remarks, setRemarks] = useState("");

  const loadData = () => {
    setLoading(true);
    setError("");
    Promise.allSettled([getActiveDeliveries(), getDeliveryHistory()])
      .then((results) => {
        if (results[0].status === "fulfilled") {
          setActiveDeliveries(results[0].value || []);
        }
        if (results[1].status === "fulfilled") {
          setHistoryDeliveries(results[1].value || []);
        }
        const failures = results.filter((r) => r.status === "rejected");
        if (failures.length) console.warn("Delivery partial load failures:", failures);
      })
      .catch((e) => console.error("Failed to load delivery data", e))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRecordArrival = async (e) => {
    e.preventDefault();
    if (!arrivalModalDelivery) return;
    setError("");
    setSuccess("");
    try {
      await recordArrival(arrivalModalDelivery.id, { receivedBy, remarks });
      setSuccess(`Arrival recorded for delivery: ${arrivalModalDelivery.deliveryNumber}`);
      setArrivalModalDelivery(null);
      setReceivedBy("");
      setRemarks("");
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to record arrival.");
    }
  };

  const handleCompleteDelivery = async (e) => {
    e.preventDefault();
    if (!completeModalDelivery) return;
    setError("");
    setSuccess("");
    try {
      await completeDelivery(completeModalDelivery.id, { completedBy, remarks });
      setSuccess(`Delivery completed successfully: ${completeModalDelivery.deliveryNumber}`);
      setCompleteModalDelivery(null);
      setCompletedBy("");
      setRemarks("");
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to complete delivery.");
    }
  };

  return (
    <RouteGuard allowedRoles={["OPERATIONS", "OPERATOR", "DISPATCHER", "ADMIN"]}>
      <DashboardLayout
        role="Operations Management"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={(key) => {
          if (key === "operations") navigate({ to: "/operations" });
          else if (key === "documents") navigate({ to: "/delivery-documents" });
          else if (key === "dispatch") navigate({ to: "/dispatch" });
          else setActiveTab(key);
        }}
      >
        <PageHeader title="Delivery Management" crumbs={["Operations", "Delivery Management", activeTab === "dash" ? "Overview" : SIDE.find((item) => item.key === activeTab)?.label || activeTab]} />
        <OperatorWorkflowProgress current="Delivery" nextLabel="View Completed Deliveries" onNext={() => setActiveTab("history")} />

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

        <div className="fef-stat-grid">
          <StatCard
            label="In Transit Trucks"
            value={loading ? "…" : String(activeDeliveries.filter(d => d.deliveryStatus === "IN_TRANSIT").length)}
            icon={FiTruck}
            tone="primary"
          />
          <StatCard
            label="Arrived at Destinations"
            value={loading ? "…" : String(activeDeliveries.filter(d => d.deliveryStatus === "ARRIVED_AT_DESTINATION").length)}
            icon={FiMapPin}
            tone="warning"
          />
          <StatCard
            label="Total Delivered (All Time)"
            value={loading ? "…" : String(historyDeliveries.filter(d => d.deliveryStatus === "COMPLETED" || d.deliveryStatus === "DELIVERED").length)}
            icon={FiCheckCircle}
            tone="success"
          />
        </div>

        {/* SUMMARY TAB */}
        {activeTab === "dash" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Delivery Workspace Summary</h3>
            </div>
            <div style={{ padding: 20, color: "var(--feftms-text-muted)" }}>
              {loading ? (
                <p>Loading delivery metrics…</p>
              ) : (
                <div>
                  <p>
                    Showing operational transits en route: <strong>{activeDeliveries.length}</strong>{" "}
                    active trips, and <strong>{historyDeliveries.length}</strong> archived records.
                  </p>
                  <p style={{ marginTop: 10 }}>
                    Select the **Active Transits** tab to inspect running fuel cargoes, mark truck arrivals, or record final receiver completions.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ACTIVE DELIVERIES TAB */}
        {activeTab === "active" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Active En-Route Transits</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Delivery / Dispatch #</th>
                    <th>Truck Number</th>
                    <th>Driver Name</th>
                    <th>Destination</th>
                    <th>Dispatch Date/Time</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {activeDeliveries.map((d) => (
                    <tr key={d.id}>
                      <td>
                        <strong>{d.deliveryNumber}</strong>
                        <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                          Disp Ref: {d.dispatchNumber}
                        </div>
                      </td>
                      <td>{d.truckNumber}</td>
                      <td>{d.driverName}</td>
                      <td>{d.destination}</td>
                      <td>
                        {d.dispatchedAt ? new Date(d.dispatchedAt).toLocaleString() : "—"}
                      </td>
                      <td>
                        <span className={`fef-badge fef-badge-${d.deliveryStatus === "ARRIVED_AT_DESTINATION" ? "warning" : "secondary"}`}>
                          {d.deliveryStatus === "ARRIVED_AT_DESTINATION" ? "ARRIVED" : "IN TRANSIT"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 6 }}>
                          <button
                            className="fef-btn fef-btn-outline"
                            style={{ padding: "4px 8px", fontSize: 11 }}
                            onClick={() => setSelectedDeliveryDetails(d)}
                          >
                            Details
                          </button>
                          
                          {d.deliveryStatus === "IN_TRANSIT" && (
                            <button
                              className="fef-btn fef-btn-primary"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => {
                                setArrivalModalDelivery(d);
                                setReceivedBy("");
                                setRemarks("");
                              }}
                            >
                              Mark Arrived
                            </button>
                          )}

                          {d.deliveryStatus === "ARRIVED_AT_DESTINATION" && (
                            <button
                              className="fef-btn fef-btn-success"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => {
                                setCompleteModalDelivery(d);
                                setCompletedBy("");
                                setRemarks("");
                              }}
                            >
                              Complete Delivery
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {activeDeliveries.length === 0 && !loading && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 25 }}>
                        <FiInfo size={24} style={{ marginBottom: 8, opacity: 0.5 }} />
                        <p>No active deliveries are available. Release a dispatch and start transit in Dispatch Management to create a delivery for tracking.</p>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* DELIVERY HISTORY TAB */}
        {activeTab === "history" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Archived Fuel Deliveries</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Delivery #</th>
                    <th>Truck Number</th>
                    <th>Driver Name</th>
                    <th>Destination</th>
                    <th>Completed At</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {historyDeliveries.map((d) => (
                    <tr key={d.id}>
                      <td>
                        <strong>{d.deliveryNumber}</strong>
                        <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                          Disp Ref: {d.dispatchNumber}
                        </div>
                      </td>
                      <td>{d.truckNumber}</td>
                      <td>{d.driverName}</td>
                      <td>{d.destination}</td>
                      <td>
                        {d.deliveredAt ? new Date(d.deliveredAt).toLocaleString() : "—"}
                      </td>
                      <td>
                        <span className={`fef-badge fef-badge-${(d.deliveryStatus === "COMPLETED" || d.deliveryStatus === "DELIVERED") ? "success" : "danger"}`}>
                          {d.deliveryStatus}
                        </span>
                      </td>
                      <td>
                        <button
                          className="fef-btn fef-btn-outline"
                          style={{ padding: "4px 8px", fontSize: 11 }}
                          onClick={() => setSelectedDeliveryDetails(d)}
                        >
                          Details
                        </button>
                      </td>
                    </tr>
                  ))}
                  {historyDeliveries.length === 0 && !loading && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 25 }}>
                        No completed deliveries yet. Record arrival and then complete the delivery from Active Transits.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* DETAILS VIEW MODAL */}
        {selectedDeliveryDetails &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setSelectedDeliveryDetails(null)}>
              <div className="fef-modal-window" style={{ maxWidth: "700px" }} onClick={(e) => e.stopPropagation()}>
                <button className="fef-modal-close" onClick={() => setSelectedDeliveryDetails(null)}>
                  <FiX />
                </button>
                <div className="fef-detail-modal-header" style={{ borderBottom: "1px solid #E5E7EB", paddingBottom: 15 }}>
                  <span className={`fef-badge fef-badge-${(selectedDeliveryDetails.deliveryStatus === "COMPLETED" || selectedDeliveryDetails.deliveryStatus === "DELIVERED") ? "success" : selectedDeliveryDetails.deliveryStatus === "ARRIVED_AT_DESTINATION" ? "warning" : "secondary"}`} style={{ marginBottom: 8, display: "inline-block" }}>
                    {selectedDeliveryDetails.deliveryStatus}
                  </span>
                  <h2>Delivery Tracking Metadata</h2>
                  <span style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>
                    Delivery ID: {selectedDeliveryDetails.deliveryNumber}
                  </span>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 25, marginTop: 20 }}>
                  <div>
                    <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiTruck style={{ verticalAlign: "-2px", marginRight: 6 }} /> Dispatch Carrier Info</h4>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Truck Number:</strong> {selectedDeliveryDetails.truckNumber}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Driver Name:</strong> {selectedDeliveryDetails.driverName}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Transport Company:</strong> {selectedDeliveryDetails.transportCompany}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Destination:</strong> {selectedDeliveryDetails.destination}</p>
                  </div>
                  <div>
                    <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiFileText style={{ verticalAlign: "-2px", marginRight: 6 }} /> Associated Documents</h4>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Dispatch Number:</strong> {selectedDeliveryDetails.dispatchNumber || "N/A"}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Delivery Note:</strong> {selectedDeliveryDetails.deliveryNoteNumber || "N/A"}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Truck Invoice:</strong> {selectedDeliveryDetails.truckInvoiceNumber || "N/A"}</p>
                  </div>
                </div>

                <div style={{ marginTop: 25, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiClock style={{ verticalAlign: "-2px", marginRight: 6 }} /> Transit Timeline Metrics</h4>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 15 }}>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Dispatched At:</strong> {selectedDeliveryDetails.dispatchedAt ? new Date(selectedDeliveryDetails.dispatchedAt).toLocaleString() : "N/A"}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Arrived At:</strong> {selectedDeliveryDetails.arrivalTime ? new Date(selectedDeliveryDetails.arrivalTime).toLocaleString() : "N/A"}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Delivered At:</strong> {selectedDeliveryDetails.deliveredAt ? new Date(selectedDeliveryDetails.deliveredAt).toLocaleString() : "N/A"}</p>
                  </div>
                </div>

                <div style={{ marginTop: 25, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiFlag style={{ verticalAlign: "-2px", marginRight: 6 }} /> Handover Audit Trail</h4>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 15 }}>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Received By:</strong> {selectedDeliveryDetails.receivedBy || "N/A"}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Completed By:</strong> {selectedDeliveryDetails.completedBy || "N/A"}</p>
                  </div>
                  {selectedDeliveryDetails.remarks && (
                    <p style={{ marginTop: 10, fontSize: 13, background: "rgba(249,115,22,0.05)", padding: 10, borderRadius: 4 }}>
                      <strong>Remarks:</strong> {selectedDeliveryDetails.remarks}
                    </p>
                  )}
                </div>

                <div style={{ display: "flex", justifyContent: "end", gap: 10, marginTop: 30, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  <button className="fef-btn fef-btn-outline" onClick={() => setSelectedDeliveryDetails(null)}>
                    Close
                  </button>
                </div>
              </div>
            </div>,
            document.body
          )}

        {/* RECORD ARRIVAL MODAL */}
        {arrivalModalDelivery &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setArrivalModalDelivery(null)}>
              <div className="fef-modal-window" style={{ maxWidth: "450px" }} onClick={(e) => e.stopPropagation()}>
                <button className="fef-modal-close" onClick={() => setArrivalModalDelivery(null)}>
                  <FiX />
                </button>
                <h3>Record Destination Arrival</h3>
                <p style={{ fontSize: 13, color: "var(--feftms-text-muted)", marginBottom: 20 }}>
                  Authorizing arrival for truck <strong>{arrivalModalDelivery.truckNumber}</strong>.
                </p>
                <form onSubmit={handleRecordArrival}>
                  <div className="fef-form-group">
                    <label>Received By (Name / Title) *</label>
                    <input
                      type="text"
                      className="fef-input"
                      value={receivedBy}
                      onChange={(e) => setReceivedBy(e.target.value)}
                      required
                      placeholder="e.g. Depot Supervisor / Gate Inspector"
                    />
                  </div>
                  <div className="fef-form-group" style={{ marginTop: 15 }}>
                    <label>Arrival Remarks</label>
                    <textarea
                      className="fef-input"
                      value={remarks}
                      onChange={(e) => setRemarks(e.target.value)}
                      placeholder="Optional remarks about delivery reception"
                      rows="3"
                    />
                  </div>
                  <div style={{ display: "flex", justifyContent: "end", gap: 10, marginTop: 25 }}>
                    <button type="button" className="fef-btn fef-btn-outline" onClick={() => setArrivalModalDelivery(null)}>
                      Cancel
                    </button>
                    <button type="submit" className="fef-btn fef-btn-primary">
                      Record Arrival
                    </button>
                  </div>
                </form>
              </div>
            </div>,
            document.body
          )}

        {/* COMPLETE DELIVERY MODAL */}
        {completeModalDelivery &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setCompleteModalDelivery(null)}>
              <div className="fef-modal-window" style={{ maxWidth: "450px" }} onClick={(e) => e.stopPropagation()}>
                <button className="fef-modal-close" onClick={() => setCompleteModalDelivery(null)}>
                  <FiX />
                </button>
                <h3>Complete Fuel Delivery</h3>
                <p style={{ fontSize: 13, color: "var(--feftms-text-muted)", marginBottom: 20 }}>
                  Confirming final fuel dump completion for <strong>{completeModalDelivery.truckNumber}</strong>.
                </p>
                <form onSubmit={handleCompleteDelivery}>
                  <div className="fef-form-group">
                    <label>Completed By (Signature Representative) *</label>
                    <input
                      type="text"
                      className="fef-input"
                      value={completedBy}
                      onChange={(e) => setCompletedBy(e.target.value)}
                      required
                      placeholder="e.g. Alice Receiver / Client Manager"
                    />
                  </div>
                  <div className="fef-form-group" style={{ marginTop: 15 }}>
                    <label>Completion Remarks</label>
                    <textarea
                      className="fef-input"
                      value={remarks}
                      onChange={(e) => setRemarks(e.target.value)}
                      placeholder="Optional delivery details, product quality check, etc."
                      rows="3"
                    />
                  </div>
                  <div style={{ display: "flex", justifyContent: "end", gap: 10, marginTop: 25 }}>
                    <button type="button" className="fef-btn fef-btn-outline" onClick={() => setCompleteModalDelivery(null)}>
                      Cancel
                    </button>
                    <button type="submit" className="fef-btn fef-btn-success">
                      Complete Delivery
                    </button>
                  </div>
                </form>
              </div>
            </div>,
            document.body
          )}
      </DashboardLayout>
    </RouteGuard>
  );
}
