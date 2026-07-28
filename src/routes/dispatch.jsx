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
  FiCheck,
  FiClipboard,
  FiInfo,
  FiArrowRight,
  FiFileText,
  FiClock,
  FiPlus,
  FiUserCheck,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { listDeliveries, updateDeliveryStatus } from "../services/deliveryService";
import { listDrivers } from "../services/driverService";
import { listVehicles } from "../services/vehicleService";
import { listLoadingOrders } from "../services/loadingOrderService";
import { getDeliveryNote, getTruckInvoice } from "../services/deliveryDocumentService";
import {
  createDispatch,
  getDispatchByActivityId,
  releaseTruck,
  startTransit,
} from "../services/dispatchService";

export const Route = createFileRoute("/dispatch")({
  head: () => ({ meta: [{ title: "Dispatcher Workspace — FEFTMS" }] }),
  component: DispatchDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "queue", label: "Dispatch Queue", icon: FiClipboard },
  { key: "deliveries", label: "Deliveries / Trips", icon: FiMapPin },
  { key: "trucks", label: "Fleet Trucks", icon: FiTruck },
];

function DispatchDash() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("dash");
  const [deliveries, setDeliveries] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [completedActivities, setCompletedActivities] = useState([]);
  const [dispatchStatusMap, setDispatchStatusMap] = useState({});
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Details modal
  const [selectedActDetails, setSelectedActDetails] = useState(null);

  const loadData = () => {
    setLoading(true);
    setError("");
    Promise.allSettled([
      listDeliveries(),
      listDrivers(),
      listVehicles(),
      listLoadingOrders(),
    ])
      .then(async (results) => {
        if (results[0].status === "fulfilled")
          setDeliveries(results[0].value.content || results[0].value || []);
        if (results[1].status === "fulfilled")
          setDrivers(results[1].value.content || results[1].value || []);
        if (results[2].status === "fulfilled")
          setVehicles(results[2].value.content || results[2].value || []);
        
        let completedActs = [];
        if (results[3].status === "fulfilled") {
          const orders = results[3].value || [];
          orders.forEach((order) => {
            if (order.activities) {
              order.activities.forEach((activity) => {
                if (activity.status === "COMPLETED" || activity.status === "DISPATCHED" || activity.status === "IN_TRANSIT") {
                  completedActs.push({
                    ...activity,
                    loadingOrderNumber: order.loadingOrderNumber,
                    loadingOrderId: order.id,
                    loadingTerminal: order.loadingTerminal,
                    consignee: order.consignee,
                  });
                }
              });
            }
          });
        }
        setCompletedActivities(completedActs);
        await fetchDispatchStatuses(completedActs);

        const failures = results.filter((r) => r.status === "rejected");
        if (failures.length) console.warn("Dispatch partial load failures:", failures);
      })
      .catch((e) => console.error("Failed to load dispatch data", e))
      .finally(() => setLoading(false));
  };

  const fetchDispatchStatuses = async (acts) => {
    const statuses = {};
    await Promise.all(
      acts.map(async (act) => {
        statuses[act.id] = { dn: null, inv: null, dispatch: null, loading: true };
        try {
          const dnRes = await getDeliveryNote(act.id);
          statuses[act.id].dn = dnRes.data || null;
        } catch (e) {}
        try {
          const invRes = await getTruckInvoice(act.id);
          statuses[act.id].inv = invRes.data || null;
        } catch (e) {}
        try {
          const dispRes = await getDispatchByActivityId(act.id);
          statuses[act.id].dispatch = dispRes.data || null;
        } catch (e) {}
        statuses[act.id].loading = false;
      })
    );
    setDispatchStatusMap(statuses);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleUpdateTripStatus = async (deliveryId, newStatus) => {
    setError("");
    setSuccess("");
    try {
      await updateDeliveryStatus(deliveryId, newStatus);
      setSuccess(`Trip marked as ${newStatus} successfully.`);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to update trip status.");
    }
  };

  const handleCreateDispatch = async (activityId) => {
    setError("");
    setSuccess("");
    try {
      const res = await createDispatch(activityId);
      setSuccess(`Dispatch record ${res.data.dispatchNumber} created successfully!`);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to initiate dispatch.");
    }
  };

  const handleReleaseTruck = async (dispatchId) => {
    setError("");
    setSuccess("");
    try {
      const res = await releaseTruck(dispatchId);
      setSuccess(`Truck has been successfully released from terminal! (Dispatch: ${res.data.dispatchNumber})`);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to release truck.");
    }
  };

  const handleStartTransit = async (dispatchId) => {
    setError("");
    setSuccess("");
    try {
      const res = await startTransit(dispatchId);
      setSuccess(`Truck transit initiated! Dispatch status set to IN_TRANSIT.`);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to update transit status.");
    }
  };

  const activeFleet = vehicles.length;
  const tripsEnRoute = deliveries.filter((d) => d.deliveryStatus === "EN_ROUTE").length;
  
  // Ready queue displays COMPLETED activities where Delivery Note status is HANDED_TO_DRIVER and Invoice exists
  const queueActivities = completedActivities.filter((act) => {
    const state = dispatchStatusMap[act.id];
    return state && state.dn && state.dn.status === "HANDED_TO_DRIVER" && state.inv;
  });

  return (
    <RouteGuard allowedRoles={["DISPATCHER", "OPERATIONS", "ADMIN", "OPERATOR"]}>
      <DashboardLayout
        role="DISPATCHER"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={setActiveTab}
      >
        <PageHeader title="Dispatcher Console" crumbs={["Dispatch", activeTab]} />

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
            label="Active Fleet"
            value={loading ? "…" : String(activeFleet)}
            icon={FiTruck}
            tone="primary"
          />
          <StatCard
            label="Trips En Route"
            value={loading ? "…" : String(tripsEnRoute)}
            icon={FiMapPin}
            tone="secondary"
          />
          <StatCard
            label="Pending Dispatch"
            value={loading ? "…" : String(queueActivities.filter(a => !dispatchStatusMap[a.id]?.dispatch).length)}
            icon={FiClipboard}
            tone="warning"
          />
        </div>

        {/* DASHBOARD SUMMARY VIEW */}
        {activeTab === "dash" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Fleet Dispatch Schedule Overview</h3>
            </div>
            <div style={{ padding: 20, color: "var(--feftms-text-muted)" }}>
              {loading ? (
                <p>Loading schedule…</p>
              ) : (
                <div>
                  <p>
                    Showing overview of system operations: <strong>{deliveries.length}</strong>{" "}
                    total deliveries, <strong>{drivers.length}</strong> registered drivers, and{" "}
                    <strong>{vehicles.length}</strong> vehicles in fleet.
                  </p>
                  <p style={{ marginTop: 10 }}>
                    Select the **Dispatch Queue** tab to release loaded trucks or **Deliveries / Trips** to track active transport.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* DISPATCH QUEUE VIEW */}
        {activeTab === "queue" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Operations Terminal Release Queue</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Order Ref / Loading #</th>
                    <th>Truck Number</th>
                    <th>Driver Name</th>
                    <th>Destination</th>
                    <th>Document Status</th>
                    <th>Dispatch Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {queueActivities.map((act) => {
                    const state = dispatchStatusMap[act.id] || { dn: null, inv: null, dispatch: null, loading: true };
                    
                    return (
                      <tr key={act.id}>
                        <td>
                          <strong>{act.loadingOrderNumber}</strong>
                          <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                            Activity ID: {act.id}
                          </div>
                        </td>
                        <td>{act.truckNumber}</td>
                        <td>{act.driverName}</td>
                        <td>{act.destination}</td>
                        <td>
                          <div style={{ display: "flex", flexDirection: "column", gap: 3 }}>
                            <span className="fef-badge fef-badge-success" style={{ fontSize: 10 }}>
                              DN: HANDED OVER
                            </span>
                            <span className="fef-badge fef-badge-success" style={{ fontSize: 10 }}>
                              INV: {state.inv?.invoiceStatus}
                            </span>
                          </div>
                        </td>
                        <td>
                          {state.loading ? (
                            <span style={{ fontSize: 12, color: "var(--feftms-text-muted)" }}>Loading…</span>
                          ) : state.dispatch ? (
                            <span className={`fef-badge fef-badge-${state.dispatch.dispatchStatus === "IN_TRANSIT" ? "secondary" : state.dispatch.dispatchStatus === "DISPATCHED" ? "success" : "warning"}`}>
                              {state.dispatch.dispatchStatus}
                            </span>
                          ) : (
                            <span className="fef-badge fef-badge-pending">NOT READY</span>
                          )}
                        </td>
                        <td>
                          <div style={{ display: "flex", gap: 6 }}>
                            {!state.dispatch && (
                              <button
                                className="fef-btn fef-btn-primary"
                                style={{ padding: "4px 8px", fontSize: 11 }}
                                onClick={() => handleCreateDispatch(act.id)}
                              >
                                Create Dispatch
                              </button>
                            )}

                            {state.dispatch && (
                              <>
                                <button
                                  className="fef-btn fef-btn-outline"
                                  style={{ padding: "4px 8px", fontSize: 11 }}
                                  onClick={() => setSelectedActDetails({ ...act, dispatch: state.dispatch, dn: state.dn, inv: state.inv })}
                                >
                                  Details
                                </button>
                                {state.dispatch.dispatchStatus === "READY" && (
                                  <button
                                    className="fef-btn fef-btn-success"
                                    style={{ padding: "4px 8px", fontSize: 11 }}
                                    onClick={() => handleReleaseTruck(state.dispatch.id)}
                                  >
                                    <FiUserCheck style={{ marginRight: 4 }} /> Release Truck
                                  </button>
                                )}
                                {state.dispatch.dispatchStatus === "DISPATCHED" && (
                                  <button
                                    className="fef-btn fef-btn-outline"
                                    style={{ padding: "4px 8px", fontSize: 11 }}
                                    onClick={() => handleStartTransit(state.dispatch.id)}
                                  >
                                    <FiArrowRight style={{ marginRight: 4 }} /> Start Transit
                                  </button>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {queueActivities.length === 0 && !loading && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 25 }}>
                        <FiInfo size={24} style={{ marginBottom: 8, opacity: 0.5 }} />
                        <p>No trucks waiting in the dispatch queue.</p>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* DELIVERIES LIST VIEW */}
        {activeTab === "deliveries" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Dispatch Deliveries Management</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Delivery #</th>
                    <th>Driver Name</th>
                    <th>Vehicle Plate</th>
                    <th>Order Details</th>
                    <th>Status</th>
                    <th>Quick Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {deliveries.map((d) => (
                    <tr key={d.id}>
                      <td>
                        <strong>{d.deliveryNumber}</strong>
                      </td>
                      <td>
                        {d.driver?.firstName} {d.driver?.lastName}
                      </td>
                      <td>{d.vehicle?.plateNumber}</td>
                      <td>
                        {d.order?.productName || "—"} ({d.order?.quantity} L)
                      </td>
                      <td>
                        <span className={`fef-badge fef-badge-${d.deliveryStatus?.toLowerCase()}`}>
                          {d.deliveryStatus}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 6 }}>
                          {d.deliveryStatus === "PENDING" && (
                            <button
                              className="fef-btn fef-btn-outline"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => handleUpdateTripStatus(d.id, "EN_ROUTE")}
                            >
                              Dispatch En Route
                            </button>
                          )}
                          {d.deliveryStatus === "EN_ROUTE" && (
                            <button
                              className="fef-btn fef-btn-outline"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => handleUpdateTripStatus(d.id, "ARRIVED")}
                            >
                              Mark Arrived
                            </button>
                          )}
                          {d.deliveryStatus === "ARRIVED" && (
                            <button
                              className="fef-btn fef-btn-outline"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => handleUpdateTripStatus(d.id, "DELIVERED")}
                            >
                              Mark Delivered
                            </button>
                          )}
                          {(d.deliveryStatus === "PENDING" ||
                            d.deliveryStatus === "EN_ROUTE" ||
                            d.deliveryStatus === "ARRIVED") && (
                            <button
                              className="fef-btn fef-btn-outline fef-btn-danger"
                              style={{
                                padding: "4px 8px",
                                fontSize: 11,
                                border: "1px solid var(--feftms-danger)",
                                color: "var(--feftms-danger)",
                              }}
                              onClick={() => handleUpdateTripStatus(d.id, "CANCELLED")}
                            >
                              Cancel
                            </button>
                          )}
                          {(d.deliveryStatus === "DELIVERED" ||
                            d.deliveryStatus === "CANCELLED") && (
                            <span style={{ fontSize: 12, color: "var(--feftms-text-muted)" }}>
                              Closed
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {deliveries.length === 0 && !loading && (
                    <tr>
                      <td
                        colSpan="6"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No deliveries registered in database.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TRUCKS LIST VIEW */}
        {activeTab === "trucks" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Fleet Vehicles Inventory</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Plate Number</th>
                    <th>Capacity (Liters)</th>
                    <th>Linked Driver</th>
                    <th>Current status</th>
                  </tr>
                </thead>
                <tbody>
                  {vehicles.map((v) => (
                    <tr key={v.id}>
                      <td>
                        <strong>{v.plateNumber}</strong>
                      </td>
                      <td>{v.capacity?.toLocaleString()} L</td>
                      <td>{v.driver ? `${v.driver.firstName} ${v.driver.lastName}` : "None"}</td>
                      <td>
                        <span
                          className={`fef-badge fef-badge-${v.currentStatus?.toLowerCase() === "active" ? "success" : v.currentStatus?.toLowerCase() === "busy" ? "warning" : "danger"}`}
                        >
                          {v.currentStatus}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {vehicles.length === 0 && !loading && (
                    <tr>
                      <td
                        colSpan="4"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No vehicles registered in fleet database.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* DISPATCH DETAILS MODAL */}
        {selectedActDetails &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setSelectedActDetails(null)}>
              <div className="fef-modal-window" style={{ maxWidth: "700px" }} onClick={(e) => e.stopPropagation()}>
                <button className="fef-modal-close" onClick={() => setSelectedActDetails(null)}>
                  <FiX />
                </button>
                <div className="fef-detail-modal-header" style={{ borderBottom: "1px solid #E5E7EB", paddingBottom: 15 }}>
                  <span className="fef-badge fef-badge-success" style={{ marginBottom: 8, display: "inline-block" }}>
                    {selectedActDetails.dispatch.dispatchStatus}
                  </span>
                  <h2>Dispatch Authorization Details</h2>
                  <span style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>
                    Dispatch Ref: #{selectedActDetails.dispatch.dispatchNumber}
                  </span>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 25, marginTop: 20 }}>
                  <div>
                    <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiTruck style={{ verticalAlign: "-2px", marginRight: 6 }} /> Truck & Driver Info</h4>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Plate Number:</strong> {selectedActDetails.dispatch.truckNumber}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Driver Name:</strong> {selectedActDetails.dispatch.driverName}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>License Number:</strong> {selectedActDetails.dispatch.driverLicenseNumber}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Transport Carrier:</strong> {selectedActDetails.dispatch.transportCompany}</p>
                  </div>
                  <div>
                    <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiMapPin style={{ verticalAlign: "-2px", marginRight: 6 }} /> Cargo & Destination</h4>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Product Type:</strong> {selectedActDetails.product}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Cargo Volume:</strong> {selectedActDetails.standardVolume?.toLocaleString()} L</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Destination:</strong> {selectedActDetails.dispatch.destination}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Terminal:</strong> {selectedActDetails.loadingTerminal}</p>
                  </div>
                </div>

                <div style={{ marginTop: 25, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiFileText style={{ verticalAlign: "-2px", marginRight: 6 }} /> Associated Documents</h4>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, background: "rgba(255,255,255,0.03)", padding: 12, borderRadius: 6 }}>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Delivery Note:</strong> {selectedActDetails.dn?.deliveryNoteNumber || "N/A"}</p>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Truck Invoice:</strong> {selectedActDetails.inv?.invoiceNumber || "N/A"}</p>
                  </div>
                </div>

                <div style={{ marginTop: 25, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  <h4 style={{ margin: "0 0 10px 0", color: "#F97316" }}><FiClock style={{ verticalAlign: "-2px", marginRight: 6 }} /> Dispatch Clearance Audit</h4>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Clearance Date:</strong> {selectedActDetails.dispatch.releasedAt ? new Date(selectedActDetails.dispatch.releasedAt).toLocaleString() : "Awaiting Release"}</p>
                    <p style={{ margin: "4px 0", fontSize: 13 }}><strong>Dispatch Officer:</strong> {selectedActDetails.dispatch.releasedBy || "None"}</p>
                  </div>
                  {selectedActDetails.dispatch.remarks && (
                    <p style={{ marginTop: 10, fontSize: 13, background: "rgba(249,115,22,0.05)", padding: 10, borderRadius: 4 }}>
                      <strong>Remarks:</strong> {selectedActDetails.dispatch.remarks}
                    </p>
                  )}
                </div>

                <div style={{ display: "flex", justifyContent: "end", gap: 10, marginTop: 30, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  {selectedActDetails.dispatch.dispatchStatus === "READY" && (
                    <button className="fef-btn fef-btn-success" onClick={() => { handleReleaseTruck(selectedActDetails.dispatch.id); setSelectedActDetails(null); }}>
                      Release Truck
                    </button>
                  )}
                  {selectedActDetails.dispatch.dispatchStatus === "DISPATCHED" && (
                    <button className="fef-btn fef-btn-outline" onClick={() => { handleStartTransit(selectedActDetails.dispatch.id); setSelectedActDetails(null); }}>
                      Start Transit
                    </button>
                  )}
                  <button className="fef-btn fef-btn-outline" onClick={() => setSelectedActDetails(null)}>
                    Close
                  </button>
                </div>
              </div>
            </div>,
            document.body
          )}
      </DashboardLayout>
    </RouteGuard>
  );
}
