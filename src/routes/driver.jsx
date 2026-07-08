import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import {
  FiTruck,
  FiNavigation,
  FiMapPin,
  FiFileText,
  FiPlay,
  FiFlag,
  FiCheckCircle,
  FiHome,
  FiAlertCircle
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { listDeliveries, updateDeliveryStatus } from "../services/deliveryService";
import { listDrivers } from "../services/driverService";
import { useAuth } from "../context/AuthContext";
import { RouteGuard } from "../components/RouteGuard";
import "../styles/forms.css";

export const Route = createFileRoute("/driver")({
  head: () => ({ meta: [{ title: "Driver — FEFTMS" }] }),
  component: DriverDash,
});

const SIDE = [
  { key: "dash", label: "Driver Console", icon: FiHome },
];

function DriverDash() {
  const { user: currentLoggedUser } = useAuth();
  const driverId = currentLoggedUser?.driverId;
  const [driverProfile, setDriverProfile] = useState(null);
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (driverId) {
      listDrivers({ size: 100 })
        .then((res) => {
          const list = res.content || [];
          const profile = list.find(d => d.id === parseInt(driverId));
          setDriverProfile(profile);
        })
        .catch((err) => console.error("Error loading driver profile:", err));
    }
  }, [driverId]);

  const loadDeliveries = () => {
    if (!driverId) {
      setDeliveries([]);
      return;
    }
    setLoading(true);
    setError("");
    listDeliveries({ driverId: parseInt(driverId), size: 50 })
      .then((res) => {
        setDeliveries(res.content || []);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Failed to fetch assigned deliveries.");
        setLoading(false);
      });
  };

  useEffect(() => {
    loadDeliveries();
  }, [driverId]);

  const handleUpdateStatus = async (deliveryId, status) => {
    setError("");
    setSuccess("");
    try {
      await updateDeliveryStatus(deliveryId, status);
      setSuccess(`Trip marked as ${status} successfully.`);
      loadDeliveries();
    } catch (err) {
      setError(err?.message || "Failed to update trip status.");
    }
  };

  const activeTrip = deliveries.find(
    (d) => d.deliveryStatus === "PENDING" || d.deliveryStatus === "EN_ROUTE" || d.deliveryStatus === "ARRIVED"
  );
  const upcomingTrips = deliveries.filter((d) => d.id !== activeTrip?.id);

  const driverName = driverProfile 
    ? `${driverProfile.firstName} ${driverProfile.lastName}` 
    : (currentLoggedUser 
        ? `${currentLoggedUser.firstName || ""} ${currentLoggedUser.lastName || ""}`.trim() || currentLoggedUser.username 
        : "Driver");

  return (
    <RouteGuard allowedRoles={["DRIVER"]}>
      <DashboardLayout
        role="Driver"
        userName={driverName}
        pageTitle="Driver Console"
        sideItems={SIDE}
        activeKey="dash"
      >
        <PageHeader title="Driver Workspace" crumbs={["Driver", "Console"]} />

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

        {!driverId ? (
          <div className="fef-panel" style={{ textAlign: "center", padding: "40px 20px" }}>
            <FiAlertCircle size={48} style={{ color: "var(--feftms-danger)", marginBottom: 15 }} />
            <h3>No Linked Driver Profile</h3>
            <p className="fef-sub">Please contact your administrator to associate your user account with a driver profile.</p>
          </div>
        ) : (
          <>
            <div className="fef-stat-grid">
              <StatCard label="Assigned Deliveries" value={deliveries.length} icon={FiTruck} tone="primary" />
              <StatCard label="Current Status" value={activeTrip?.deliveryStatus || "Idle"} icon={FiNavigation} tone="accent" />
              <StatCard label="Driver Name" value={driverName} icon={FiMapPin} tone="secondary" />
              <StatCard label="License Number" value={driverProfile?.licenseNumber || "—"} icon={FiFileText} tone="success" />
            </div>

            {/* ACTIVE TRIP */}
            {activeTrip ? (
              <div className="fef-panel" style={{ marginTop: 24 }}>
                <div className="fef-panel-head">
                  <h3>Active Trip: {activeTrip.deliveryNumber}</h3>
                </div>
                <div className="fef-table-wrap">
                  <table className="fef-table">
                    <thead>
                      <tr>
                        <th>Customer</th>
                        <th>Fuel Product</th>
                        <th>Volume</th>
                        <th>Order Status</th>
                        <th>Trip Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>{activeTrip.order?.customerName || "—"}</td>
                        <td>{activeTrip.order?.productName || "—"}</td>
                        <td>{activeTrip.order?.quantity?.toLocaleString()} L</td>
                        <td>{activeTrip.order?.orderStatus}</td>
                        <td>
                          <span className={`fef-badge fef-badge-${activeTrip.deliveryStatus?.toLowerCase()}`}>
                            {activeTrip.deliveryStatus}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div style={{ padding: "16px 20px", display: "flex", gap: 10, background: "var(--feftms-bg-alt)", borderTop: "1px solid var(--feftms-border)" }}>
                  {activeTrip.deliveryStatus === "PENDING" && (
                    <button
                      className="fef-btn fef-btn-primary"
                      onClick={() => handleUpdateStatus(activeTrip.id, "EN_ROUTE")}
                    >
                      <FiPlay style={{ marginRight: 6 }} /> Start Delivery (En Route)
                    </button>
                  )}
                  {activeTrip.deliveryStatus === "EN_ROUTE" && (
                    <button
                      className="fef-btn fef-btn-accent"
                      onClick={() => handleUpdateStatus(activeTrip.id, "ARRIVED")}
                    >
                      <FiMapPin style={{ marginRight: 6 }} /> Arrived at Destination
                    </button>
                  )}
                  {activeTrip.deliveryStatus === "ARRIVED" && (
                    <button
                      className="fef-btn fef-btn-success"
                      onClick={() => handleUpdateStatus(activeTrip.id, "DELIVERED")}
                    >
                      <FiCheckCircle style={{ marginRight: 6 }} /> Complete Delivery (Delivered)
                    </button>
                  )}
                  {(activeTrip.deliveryStatus === "PENDING" || activeTrip.deliveryStatus === "EN_ROUTE" || activeTrip.deliveryStatus === "ARRIVED") && (
                    <button
                      className="fef-btn fef-btn-danger"
                      onClick={() => handleUpdateStatus(activeTrip.id, "CANCELLED")}
                    >
                      <FiFlag style={{ marginRight: 6 }} /> Cancel / Abort Delivery
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <div className="fef-panel" style={{ marginTop: 24, padding: "30px 20px", textAlign: "center" }}>
                <FiCheckCircle size={36} style={{ color: "var(--feftms-success)", marginBottom: 10 }} />
                <h4>No Active Deliveries</h4>
                <p className="fef-sub">You are currently offline or have completed all assigned routes.</p>
              </div>
            )}

            {/* UPCOMING / HISTORIC TRIPS */}
            <div className="fef-panel" style={{ marginTop: 24 }}>
              <div className="fef-panel-head">
                <h3>Other Assigned Deliveries</h3>
              </div>
              <div className="fef-table-wrap">
                <table className="fef-table">
                  <thead>
                    <tr>
                      <th>Delivery #</th>
                      <th>Customer</th>
                      <th>Volume</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {upcomingTrips.map((t) => (
                      <tr key={t.id}>
                        <td>{t.deliveryNumber}</td>
                        <td>{t.order?.customerName}</td>
                        <td>{t.order?.quantity?.toLocaleString()} L</td>
                        <td>
                          <span className={`fef-badge fef-badge-${t.deliveryStatus?.toLowerCase()}`}>
                            {t.deliveryStatus}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {upcomingTrips.length === 0 && (
                      <tr>
                        <td colSpan="4" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                          No upcoming assignments scheduled.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </DashboardLayout>
    </RouteGuard>
  );
}
