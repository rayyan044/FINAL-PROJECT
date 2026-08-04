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
import { OperatorWorkflowProgress } from "../components/OperatorWorkflowProgress";
import { listDrivers } from "../services/driverService";
import { listVehicles } from "../services/vehicleService";
import { listLoadingOrders } from "../services/loadingOrderService";
import {
  getDispatchByActivityId,
  releaseTruck,
  cancelDispatch,
  startTransit,
} from "../services/dispatchService";
import { getDeliveryNote, getPaymentReceiptForOrder } from "../services/deliveryDocumentService";

export const Route = createFileRoute("/dispatch")({
  head: () => ({ meta: [{ title: "Dispatcher Workspace — FEFTMS" }] }),
  component: DispatchDash,
});

const SIDE = [
  { key: "operations", label: "Operations", icon: FiHome },
  { key: "documents", label: "Delivery Documents", icon: FiFileText },
  { key: "dash", label: "Dispatch Management", icon: FiHome },
  { key: "queue", label: "Dispatch Queue", icon: FiClipboard },
  { key: "deliveries", label: "Delivery Management", icon: FiMapPin },
  { key: "trucks", label: "Fleet Trucks", icon: FiTruck },
];

function DispatchDash() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("dash");
  const [drivers, setDrivers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [completedActivities, setCompletedActivities] = useState([]);
  const [dispatchStatusMap, setDispatchStatusMap] = useState({});
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  // Details modal
  const [selectedActDetails, setSelectedActDetails] = useState(null);

  const loadData = () => {
    setLoading(true);
    setError("");
    Promise.allSettled([
      listDrivers(),
      listVehicles(),
      listLoadingOrders(),
    ])
      .then(async (results) => {
        if (results[0].status === "fulfilled")
          setDrivers(results[0].value.content || results[0].value || []);
        if (results[1].status === "fulfilled")
          setVehicles(results[1].value.content || results[1].value || []);
        
        let completedActs = [];
        if (results[2].status === "fulfilled") {
          const orders = results[2].value || [];
          orders.forEach((order) => {
            if (order.activities) {
              order.activities.forEach((activity) => {
                if (activity.status === "COMPLETED" || activity.status === "DISPATCHED" || activity.status === "IN_TRANSIT") {
                  completedActs.push({
                    ...activity,
                    customerName: order.customerName,
                    customerOrderNumber: order.customerOrderNumber,
                    orderId: order.orderId,
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
        statuses[act.id] = { dn: null, receipt: null, dispatch: null, loading: true };
        try {
          const dispRes = await getDispatchByActivityId(act.id);
          statuses[act.id].dispatch = dispRes || null;
        } catch (e) {}
        try { statuses[act.id].dn = await getDeliveryNote(act.id); } catch (e) {}
        try { statuses[act.id].receipt = await getPaymentReceiptForOrder(act.orderId); } catch (e) {}
        statuses[act.id].loading = false;
      })
    );
    setDispatchStatusMap(statuses);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleReleaseTruck = async (dispatchId) => {
    setError("");
    setSuccess("");
    try {
      const res = await releaseTruck(dispatchId);
      setSuccess(`Truck has been successfully released from terminal! (Dispatch: ${res.dispatchNumber})`);
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

  const handleCancelDispatch = async (dispatchId) => {
    if (!window.confirm("Cancel this READY dispatch? The truck will not be released.")) return;
    setError("");
    setSuccess("");
    try {
      const res = await cancelDispatch(dispatchId);
      setSuccess(`Dispatch ${res.dispatchNumber} has been cancelled.`);
      setSelectedActDetails(null);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to cancel dispatch.");
    }
  };

  const activeFleet = vehicles.length;
  const getDispatchCustomerName = (activity) => {
    const isEmergencyRequest = activity.customerName === "Stranded Drivers (Emergency Requests)" || activity.customerName === "Customer Fuel Requests";
    return isEmergencyRequest && activity.consignee ? activity.consignee : activity.customerName;
  };
  
  const queueActivities = completedActivities.filter((act) => dispatchStatusMap[act.id]?.dispatch);
  const readyToCreateDispatch = queueActivities.filter((activity) => dispatchStatusMap[activity.id]?.dispatch?.dispatchStatus === "READY");
  const releasedOrInTransit = queueActivities.filter((activity) => ["DISPATCHED", "IN_TRANSIT"].includes(dispatchStatusMap[activity.id]?.dispatch?.dispatchStatus));
  const displayedQueue = queueActivities.filter((act) => { const dispatch = dispatchStatusMap[act.id]?.dispatch; const haystack = `${getDispatchCustomerName(act)} ${act.customerOrderNumber} ${act.truckNumber} ${act.driverName} ${dispatch?.dispatchNumber}`.toLowerCase(); return (statusFilter === "ALL" || dispatch?.dispatchStatus === statusFilter) && haystack.includes(search.toLowerCase()); });

  return (
    <RouteGuard allowedRoles={["DISPATCHER", "OPERATIONS", "ADMIN", "OPERATOR"]}>
      <DashboardLayout
        role="DISPATCHER"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={(key) => {
          if (key === "operations") navigate({ to: "/operations" });
          else if (key === "documents") navigate({ to: "/delivery-documents" });
          else if (key === "deliveries") navigate({ to: "/deliveries" });
          else setActiveTab(key);
        }}
      >
        <PageHeader title="Dispatch Management" crumbs={["Operations", "Dispatch Management", activeTab === "dash" ? "Overview" : SIDE.find((item) => item.key === activeTab)?.label || activeTab]} />
        <OperatorWorkflowProgress current="Dispatch" nextLabel="Delivery Management" onNext={() => navigate({ to: "/deliveries" })} />

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
            label="Ready for Release"
            value={loading ? "…" : String(readyToCreateDispatch.length)}
            icon={FiMapPin}
            tone="secondary"
          />
          <StatCard
            label="Released / In Transit"
            value={loading ? "…" : String(releasedOrInTransit.length)}
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
                    Showing the terminal release workflow: <strong>{queueActivities.length}</strong>{" "}
                    eligible dispatch activities, <strong>{drivers.length}</strong> registered drivers, and{" "}
                    <strong>{vehicles.length}</strong> vehicles in fleet.
                  </p>
                  <p style={{ marginTop: 10 }}>
                    Dispatch records are created automatically after detailed loading completion and its loading report generation. Confirm the READY dispatch to release the truck.
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
              <div style={{ display: "flex", gap: 8 }}><input className="fef-input" placeholder="Search customer, order, truck…" value={search} onChange={(e) => setSearch(e.target.value)} style={{ minWidth: 210, height: 32 }} /><select className="fef-input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} style={{ height: 32 }}><option value="ALL">All statuses</option><option value="READY">READY</option><option value="DISPATCHED">DISPATCHED</option><option value="IN_TRANSIT">IN TRANSIT</option><option value="CANCELLED">CANCELLED</option></select></div>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Customer / Order</th>
                    <th>Dispatch / Documents</th>
                    <th>Truck Number</th>
                    <th>Driver Name</th>
                    <th>Destination</th>
                    <th>Loading Status</th>
                    <th>Dispatch Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {displayedQueue.map((act) => {
                    const state = dispatchStatusMap[act.id] || { dn: null, inv: null, dispatch: null, loading: true };
                    
                    return (
                      <tr key={act.id}>
                        <td>
                          <strong>{getDispatchCustomerName(act)}</strong>
                          <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                            {act.customerOrderNumber} · {act.loadingOrderNumber}
                          </div>
                        </td>
                        <td><strong>{state.dispatch?.dispatchNumber || "Creating"}</strong><div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>DN: {state.dn?.deliveryNoteNumber || "Missing"} · Report: {act.reports?.[0]?.reportNumber || "Missing"}</div></td>
                        <td>{act.truckNumber}</td>
                        <td>{act.driverName}</td>
                        <td>{act.destination}</td>
                        <td>
                          <span className="fef-badge fef-badge-success" style={{ fontSize: 10 }}>LOADING REPORT COMPLETED</span>
                        </td>
                        <td>
                          {state.loading ? (
                            <span style={{ fontSize: 12, color: "var(--feftms-text-muted)" }}>Loading…</span>
                          ) : state.dispatch ? (
                            <span className={`fef-badge fef-badge-${state.dispatch.dispatchStatus === "IN_TRANSIT" ? "secondary" : state.dispatch.dispatchStatus === "DISPATCHED" ? "success" : "warning"}`}>
                              {state.dispatch.dispatchStatus}
                            </span>
                          ) : <span className="fef-badge fef-badge-pending">CREATING</span>}
                        </td>
                        <td>
                          <div style={{ display: "flex", gap: 6 }}>
                            {state.dispatch && (
                              <>
                                <button
                                  className="fef-btn fef-btn-outline"
                                  style={{ padding: "4px 8px", fontSize: 11 }}
                                  onClick={() => setSelectedActDetails({ ...act, dispatch: state.dispatch, dn: state.dn, receipt: state.receipt, release: state.release })}
                                >
                                  Details
                                </button>
                                {state.dispatch.dispatchStatus === "READY" && (
                                  <>
                                    <button className="fef-btn fef-btn-success" style={{ padding: "4px 8px", fontSize: 11 }} onClick={() => handleReleaseTruck(state.dispatch.id)}><FiUserCheck style={{ marginRight: 4 }} /> Release Truck</button>
                                    <button className="fef-btn fef-btn-outline" style={{ padding: "4px 8px", fontSize: 11 }} onClick={() => handleCancelDispatch(state.dispatch.id)}>Cancel</button>
                                  </>
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
                  {displayedQueue.length === 0 && !loading && (
                    <tr>
                    <td colSpan="8" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 25 }}>
                        <FiInfo size={24} style={{ marginBottom: 8, opacity: 0.5 }} />
                        <p>No completed loading reports are waiting for dispatch.</p>
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
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, background: "rgba(255,255,255,0.03)", padding: 12, borderRadius: 6 }}>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Customer:</strong> {selectedActDetails.customerName}</p>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Customer Order:</strong> {selectedActDetails.customerOrderNumber}</p>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Delivery Note:</strong> {selectedActDetails.dn?.deliveryNoteNumber || "Missing"}</p>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Payment Receipt:</strong> {selectedActDetails.receipt?.receiptNumber || "Missing"}</p>
                    <p style={{ margin: 0, fontSize: 13 }}><strong>Loading Report:</strong> {selectedActDetails.reports?.[0]?.reportNumber || "Missing"}</p>
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
                    <>
                      <button className="fef-btn fef-btn-success" onClick={() => { handleReleaseTruck(selectedActDetails.dispatch.id); setSelectedActDetails(null); }}>Release Truck</button>
                      <button className="fef-btn fef-btn-outline" onClick={() => handleCancelDispatch(selectedActDetails.dispatch.id)}>Cancel Dispatch</button>
                    </>
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
