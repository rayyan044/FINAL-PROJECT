import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { FiHome, FiTruck, FiMapPin, FiCheckCircle, FiAlertCircle, FiX, FiCheck } from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { listDeliveries, updateDeliveryStatus } from "../services/deliveryService";
import { listDrivers } from "../services/driverService";
import { listVehicles } from "../services/vehicleService";

export const Route = createFileRoute("/dispatch")({
  head: () => ({ meta: [{ title: "Dispatcher Workspace — FEFTMS" }] }),
  component: DispatchDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "deliveries", label: "Deliveries", icon: FiMapPin },
  { key: "trucks", label: "Trucks", icon: FiTruck },
];

function DispatchDash() {
  const [activeTab, setActiveTab] = useState("dash");
  const [deliveries, setDeliveries] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = () => {
    setLoading(true);
    setError("");
    Promise.allSettled([listDeliveries(), listDrivers(), listVehicles()])
      .then((results) => {
        if (results[0].status === "fulfilled") setDeliveries(results[0].value.content || results[0].value || []);
        if (results[1].status === "fulfilled") setDrivers(results[1].value.content || results[1].value || []);
        if (results[2].status === "fulfilled") setVehicles(results[2].value.content || results[2].value || []);
        const failures = results.filter((r) => r.status === "rejected");
        if (failures.length) console.warn("Dispatch partial load failures:", failures);
      })
      .catch((e) => console.error("Failed to load dispatch data", e))
      .finally(() => setLoading(false));
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

  const activeFleet = vehicles.length;
  const tripsEnRoute = deliveries.filter((d) => d.deliveryStatus === "EN_ROUTE").length;
  const completedToday = deliveries.filter((d) => d.deliveryStatus === "DELIVERED").length;

  return (
    <RouteGuard allowedRoles={["DISPATCHER"]}>
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
          <StatCard label="Active Fleet" value={loading ? "…" : String(activeFleet)} icon={FiTruck} tone="primary" />
          <StatCard label="Trips En Route" value={loading ? "…" : String(tripsEnRoute)} icon={FiMapPin} tone="secondary" />
          <StatCard label="Completed Today" value={loading ? "…" : String(completedToday)} icon={FiCheckCircle} tone="success" />
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
                  <p>Showing overview of system operations: <strong>{deliveries.length}</strong> total deliveries, <strong>{drivers.length}</strong> registered drivers, and <strong>{vehicles.length}</strong> vehicles in fleet.</p>
                  <p style={{ marginTop: 10 }}>Use the side navigation to manage active deliveries or inspect trucks.</p>
                </div>
              )}
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
                      <td><strong>{d.deliveryNumber}</strong></td>
                      <td>{d.driver?.firstName} {d.driver?.lastName}</td>
                      <td>{d.vehicle?.plateNumber}</td>
                      <td>{d.order?.productName || "—"} ({d.order?.quantity} L)</td>
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
                          {(d.deliveryStatus === "PENDING" || d.deliveryStatus === "EN_ROUTE" || d.deliveryStatus === "ARRIVED") && (
                            <button
                              className="fef-btn fef-btn-outline fef-btn-danger"
                              style={{ padding: "4px 8px", fontSize: 11, border: "1px solid var(--feftms-danger)", color: "var(--feftms-danger)" }}
                              onClick={() => handleUpdateTripStatus(d.id, "CANCELLED")}
                            >
                              Cancel
                            </button>
                          )}
                          {(d.deliveryStatus === "DELIVERED" || d.deliveryStatus === "CANCELLED") && (
                            <span style={{ fontSize: 12, color: "var(--feftms-text-muted)" }}>Closed</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {deliveries.length === 0 && !loading && (
                    <tr>
                      <td colSpan="6" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
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
                      <td><strong>{v.plateNumber}</strong></td>
                      <td>{v.capacity?.toLocaleString()} L</td>
                      <td>{v.driver ? `${v.driver.firstName} ${v.driver.lastName}` : "None"}</td>
                      <td>
                        <span className={`fef-badge fef-badge-${v.currentStatus?.toLowerCase() === 'active' ? 'success' : v.currentStatus?.toLowerCase() === 'busy' ? 'warning' : 'danger'}`}>
                          {v.currentStatus}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {vehicles.length === 0 && !loading && (
                    <tr>
                      <td colSpan="4" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                        No vehicles registered in fleet database.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

      </DashboardLayout>
    </RouteGuard>
  );
}
