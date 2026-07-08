import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import {
  FiPackage,
  FiUserPlus,
  FiTruck,
  FiNavigation,
  FiHome,
  FiPlus,
  FiCheck,
  FiAlertCircle,
  FiCheckCircle,
  FiX,
  FiActivity
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { listRequests } from "../services/requestService";
import { listDeliveries, createDelivery } from "../services/deliveryService";
import { listDrivers, createDriver } from "../services/driverService";
import { listVehicles, createVehicle } from "../services/vehicleService";
import { listProducts, updateProduct, createProduct, deleteProduct } from "../services/productService";
import { listTanks } from "../services/tankService";
import "../styles/forms.css";

export const Route = createFileRoute("/operations")({
  head: () => ({ meta: [{ title: "Operations — FEFTMS" }] }),
  component: OpsDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "inventory", label: "Inventory Control", icon: FiPackage },
  { key: "loading", label: "Loading Orders", icon: FiCheckCircle },
  { key: "deliveries", label: "Deliveries", icon: FiNavigation },
  { key: "drivers", label: "Drivers", icon: FiUserPlus },
  { key: "vehicles", label: "Vehicles", icon: FiTruck },
];

function OpsDash() {
  const [activeTab, setActiveTab] = useState("dash");
  const [stats, setStats] = useState({ loadingOrders: 0, waitingDrivers: 0, deliveries: 0, vehicles: 0 });
  const [ordersList, setOrdersList] = useState([]);
  const [deliveriesList, setDeliveriesList] = useState([]);
  const [driversList, setDriversList] = useState([]);
  const [vehiclesList, setVehiclesList] = useState([]);
  const [productsList, setProductsList] = useState([]);
  const [tanksList, setTanksList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Sub-tab modal forms
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [assignForm, setAssignForm] = useState({ driverId: "", vehicleId: "" });

  const [showDriverModal, setShowDriverModal] = useState(false);
  const [driverForm, setDriverForm] = useState({ firstName: "", lastName: "", phone: "", licenseNumber: "" });

  const [showVehicleModal, setShowVehicleModal] = useState(false);
  const [vehicleForm, setVehicleForm] = useState({ plateNumber: "", capacity: "", driverId: "" });

  const [showStockModal, setShowStockModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [stockForm, setStockForm] = useState({
    actionType: "ADD", // ADD, SUBTRACT, STATUS_ONLY
    quantity: "",
    overrideStatus: ""
  });

  const [showCreateProductModal, setShowCreateProductModal] = useState(false);
  const [createProductForm, setCreateProductForm] = useState({
    productName: "",
    fuelType: "",
    density: "",
    availableQuantity: "0"
  });

  const loadData = () => {
    setLoading(true);
    setError("");

    Promise.allSettled([
      listRequests({ size: 100 }),
      listDeliveries({ size: 100 }),
      listDrivers({ size: 100 }),
      listVehicles({ size: 100 }),
      listProducts({ size: 100 }),
      listTanks({ size: 100 })
    ])
      .then(([reqsRes, delsRes, drvsRes, vehsRes, prodsRes, tanksRes]) => {
        const failures = [];

        if (reqsRes.status === "fulfilled") {
          const reqs = reqsRes.value;
          const approvedOrders = (reqs.content || []).filter(o => o.orderStatus === "APPROVED");
          setStats((prev) => ({
            ...prev,
            loadingOrders: approvedOrders.length
          }));
          setOrdersList(reqs.content || []);
        } else {
          failures.push({ name: "requests", error: reqsRes.reason });
          setOrdersList([]);
        }

        if (delsRes.status === "fulfilled") {
          const dels = delsRes.value;
          setStats((prev) => ({
            ...prev,
            deliveries: dels.totalElements || 0
          }));
          setDeliveriesList(dels.content || []);
        } else {
          failures.push({ name: "deliveries", error: delsRes.reason });
          setDeliveriesList([]);
        }

        if (drvsRes.status === "fulfilled") {
          const drvs = drvsRes.value;
          const availableDrivers = (drvs.content || []).filter(d => d.status === "AVAILABLE");
          setStats((prev) => ({
            ...prev,
            waitingDrivers: availableDrivers.length
          }));
          setDriversList(drvs.content || []);
        } else {
          failures.push({ name: "drivers", error: drvsRes.reason });
          setDriversList([]);
        }

        if (vehsRes.status === "fulfilled") {
          const vehs = vehsRes.value;
          setStats((prev) => ({
            ...prev,
            vehicles: vehs.totalElements || 0
          }));
          setVehiclesList(vehs.content || []);
        } else {
          failures.push({ name: "vehicles", error: vehsRes.reason });
          setVehiclesList([]);
        }

        if (prodsRes.status === "fulfilled") {
          setProductsList(prodsRes.value.content || []);
        } else {
          failures.push({ name: "products", error: prodsRes.reason });
          setProductsList([]);
        }

        if (tanksRes.status === "fulfilled") {
          setTanksList(tanksRes.value.content || []);
        } else {
          failures.push({ name: "tanks", error: tanksRes.reason });
          setTanksList([]);
        }

        if (failures.length > 0) {
          const failureNames = failures.map((failure) => failure.name).join(", ");
          setError(`Partial data load failed for: ${failureNames}. Some panels may be unavailable.`);
          failures.forEach((failure) => console.warn(`Failed to load ${failure.name}:`, failure.error));
        }

        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Unexpected error connecting to database.");
        setLoading(false);
      });
  };

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const handleOpenAssign = (order) => {
    setSelectedOrder(order);
    setAssignForm({ driverId: "", vehicleId: "" });
    setShowAssignModal(true);
  };

  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const delNumber = "DEL-" + Math.floor(100000 + Math.random() * 900000);
      await createDelivery({
        deliveryNumber: delNumber,
        orderId: selectedOrder.id,
        driverId: parseInt(assignForm.driverId),
        vehicleId: parseInt(assignForm.vehicleId),
        deliveryStatus: "PENDING"
      });
      setSuccess("Driver and vehicle assigned successfully. Dispatch logged.");
      setShowAssignModal(false);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to assign driver. Ensure vehicle and driver are available.");
    }
  };

  const handleCreateDriver = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      await createDriver({ ...driverForm, status: "AVAILABLE" });
      setSuccess("Driver profile registered successfully");
      setShowDriverModal(false);
      setDriverForm({ firstName: "", lastName: "", phone: "", licenseNumber: "" });
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to register driver. Ensure license number is unique.");
    }
  };

  const handleCreateVehicle = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      await createVehicle({
        plateNumber: vehicleForm.plateNumber,
        capacity: parseFloat(vehicleForm.capacity),
        driverId: vehicleForm.driverId ? parseInt(vehicleForm.driverId) : null,
        currentStatus: "ACTIVE"
      });
      setSuccess("Vehicle registered successfully");
      setShowVehicleModal(false);
      setVehicleForm({ plateNumber: "", capacity: "", driverId: "" });
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to register vehicle. Ensure plate number is unique.");
    }
  };

  const handleOpenStockModal = (prod) => {
    setSelectedProduct(prod);
    setStockForm({
      actionType: "ADD",
      quantity: "",
      overrideStatus: prod.status
    });
    setShowStockModal(true);
  };

  const handleStockSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    if (!selectedProduct) return;

    try {
      let finalQty = selectedProduct.availableQuantity;
      const changeQty = parseFloat(stockForm.quantity);

      if (stockForm.actionType !== "STATUS_ONLY") {
        if (isNaN(changeQty) || changeQty <= 0) {
          throw new Error("Quantity must be a positive number.");
        }
        
        if (stockForm.actionType === "ADD") {
          finalQty += changeQty;
        } else if (stockForm.actionType === "SUBTRACT") {
          if (finalQty < changeQty) {
            throw new Error(`Cannot subtract more than available stock (${finalQty} L).`);
          }
          finalQty -= changeQty;
        }
      }

      await updateProduct(selectedProduct.id, {
        productName: selectedProduct.productName,
        fuelType: selectedProduct.fuelType,
        density: selectedProduct.density,
        availableQuantity: finalQty,
        status: stockForm.overrideStatus || (finalQty > 0 ? "ACTIVE" : "UNAVAILABLE")
      });

      setSuccess(`Inventory for ${selectedProduct.productName} updated successfully.`);
      setShowStockModal(false);
      setSelectedProduct(null);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to update inventory.");
    }
  };

  const handleDeleteProduct = async (prod) => {
    if (!window.confirm(`Delete ${prod.productName}? This will remove it from all lists.`)) {
      return;
    }

    setError("");
    setSuccess("");
    try {
      await deleteProduct(prod.id);
      setSuccess(`Fuel product ${prod.productName} deleted successfully.`);
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to delete fuel product.");
    }
  };

  const handleCreateProduct = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    try {
      const density = parseFloat(createProductForm.density);
      const qty = parseFloat(createProductForm.availableQuantity);

      if (!createProductForm.productName.trim()) {
        throw new Error("Product name is required.");
      }
      if (!createProductForm.fuelType.trim()) {
        throw new Error("Fuel type is required.");
      }
      if (isNaN(density) || density <= 0) {
        throw new Error("Density must be a positive number.");
      }
      if (isNaN(qty) || qty < 0) {
        throw new Error("Initial quantity must be a non-negative number.");
      }

      await createProduct({
        productName: createProductForm.productName,
        fuelType: createProductForm.fuelType,
        density: density,
        availableQuantity: qty,
        unitPrice: 0, // Operators don't set price; Finance will do that
        status: qty > 0 ? "ACTIVE" : "UNAVAILABLE"
      });

      setSuccess(`Fuel product "${createProductForm.productName}" created successfully. Finance can now set the price.`);
      setShowCreateProductModal(false);
      setCreateProductForm({ productName: "", fuelType: "", density: "", availableQuantity: "0" });
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to create fuel product. Ensure product name is unique.");
    }
  };

  return (
    <RouteGuard allowedRoles={["OPERATIONS", "OPERATOR"]}>
      <DashboardLayout
        role="Operations Manager"
      userName="James Otieno"
      pageTitle="Operations Control"
      sideItems={SIDE}
      activeKey={activeTab}
      onSelect={setActiveTab}
    >
      <PageHeader title={
        activeTab === "dash" ? "Operations Overview" :
        activeTab === "inventory" ? "Inventory Control" :
        activeTab === "loading" ? "Approved Loading Orders" :
        activeTab === "deliveries" ? "Active Deliveries" :
        activeTab === "drivers" ? "Drivers Directory" :
        "Vehicles Fleet"
      } crumbs={["Operations", activeTab]} />

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

      {/* DASHBOARD */}
      {activeTab === "dash" && (
        <>
          <div className="fef-stat-grid">
            <StatCard label="Approved Loading Orders" value={stats.loadingOrders} icon={FiPackage} tone="primary" />
            <StatCard label="Available Drivers" value={stats.waitingDrivers} icon={FiUserPlus} tone="accent" />
            <StatCard label="Total Deliveries" value={stats.deliveries} icon={FiNavigation} tone="secondary" />
            <StatCard label="Fleet Vehicles" value={stats.vehicles} icon={FiTruck} tone="success" />
          </div>

          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Active Deliveries in System</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Delivery #</th>
                    <th>Driver</th>
                    <th>Vehicle</th>
                    <th>Order Ref</th>
                    <th>Delivery Status</th>
                  </tr>
                </thead>
                <tbody>
                  {deliveriesList.map((d) => (
                    <tr key={d.id}>
                      <td>{d.deliveryNumber}</td>
                      <td>{d.driverName}</td>
                      <td>{d.vehiclePlateNumber}</td>
                      <td>{d.orderNumber}</td>
                      <td>
                        <span className={`fef-badge fef-badge-${d.deliveryStatus?.toLowerCase()}`}>
                          {d.deliveryStatus}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {deliveriesList.length === 0 && (
                    <tr>
                      <td colSpan="5" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                        No deliveries en route. Approve and load orders to dispatch.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* LOADING ORDERS */}
      {activeTab === "loading" && (
        <div className="fef-panel">
          <div className="fef-panel-head">
            <h3>Approved Orders Awaiting Dispatch</h3>
          </div>

          {showAssignModal && selectedOrder && (
            <div className="fef-card" style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}>
              <h4>Assign Driver & Vehicle for {selectedOrder.orderNumber}</h4>
              <form onSubmit={handleAssignSubmit} style={{ marginTop: 15 }}>
                <div className="fef-form-grid">
                  <div className="fef-field">
                    <label className="fef-label">Select Driver</label>
                    <select required className="fef-select" value={assignForm.driverId} onChange={(e) => setAssignForm({ ...assignForm, driverId: e.target.value })}>
                      <option value="">-- Select Available Driver --</option>
                      {driversList
                        .filter(d => d.status === "AVAILABLE")
                        .map(d => (
                          <option key={d.id} value={d.id}>{d.firstName} {d.lastName} (Lic: {d.licenseNumber})</option>
                        ))}
                    </select>
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Select Vehicle</label>
                    <select required className="fef-select" value={assignForm.vehicleId} onChange={(e) => setAssignForm({ ...assignForm, vehicleId: e.target.value })}>
                      <option value="">-- Select Active Vehicle --</option>
                      {vehiclesList
                        .filter(v => v.currentStatus === "ACTIVE")
                        .map(v => (
                          <option key={v.id} value={v.id}>{v.plateNumber} (Cap: {v.capacity}L)</option>
                        ))}
                    </select>
                  </div>
                </div>
                <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                  <button type="submit" className="fef-btn fef-btn-primary">Dispatch Delivery</button>
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowAssignModal(false)}>Cancel</button>
                </div>
              </form>
            </div>
          )}

          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Customer</th>
                  <th>Fuel Product</th>
                  <th>Qty (L)</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {ordersList
                  .filter(o => o.orderStatus === "APPROVED")
                  .map(o => (
                    <tr key={o.id}>
                      <td>{o.orderNumber}</td>
                      <td>
                        <div>{o.customerName}</div>
                        {o.emergencyLevel && (
                          <span 
                            className={`fef-badge fef-badge-${o.emergencyLevel.toLowerCase().includes('critical') ? 'danger' : 'info'}`} 
                            style={{ fontSize: 10, padding: '2px 6px', marginTop: 4, display: 'inline-block' }}
                          >
                            {o.emergencyLevel}
                          </span>
                        )}
                        {o.driverName && (
                          <div style={{ fontSize: 11, color: 'var(--feftms-text-muted)', marginTop: 4 }}>
                            Driver: {o.driverName} ({o.driverPhone})
                          </div>
                        )}
                        {o.locationAddress && (
                          <div style={{ fontSize: 10, color: 'var(--feftms-text-muted)', marginTop: 2 }} title={`GPS: ${o.locationGps || 'N/A'} | Landmark: ${o.locationLandmark || 'N/A'}`}>
                            📍 {o.locationAddress}
                          </div>
                        )}
                      </td>
                      <td>{o.productName}</td>
                      <td>{o.quantity?.toLocaleString()}</td>
                      <td>
                        <button className="fef-btn fef-btn-primary" onClick={() => handleOpenAssign(o)}>
                          Assign & Dispatch
                        </button>
                      </td>
                    </tr>
                  ))}
                {ordersList.filter(o => o.orderStatus === "APPROVED").length === 0 && (
                  <tr>
                    <td colSpan="5" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 20 }}>
                      No approved orders awaiting dispatch.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* DELIVERIES */}
      {activeTab === "deliveries" && (
        <div className="fef-panel">
          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Delivery #</th>
                  <th>Driver</th>
                  <th>Vehicle</th>
                  <th>Order Ref</th>
                  <th>Customer</th>
                  <th>Volume</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {deliveriesList.map((d) => (
                  <tr key={d.id}>
                    <td>{d.deliveryNumber}</td>
                    <td>{d.driverName}</td>
                    <td>{d.vehiclePlateNumber}</td>
                    <td>{d.orderNumber}</td>
                    <td>
                      <div>{d.customerName}</div>
                      {d.order?.emergencyLevel && (
                        <span 
                          className={`fef-badge fef-badge-${d.order.emergencyLevel.toLowerCase().includes('critical') ? 'danger' : 'info'}`} 
                          style={{ fontSize: 10, padding: '2px 6px', marginTop: 4, display: 'inline-block' }}
                        >
                          {d.order.emergencyLevel}
                        </span>
                      )}
                      {d.order?.driverName && (
                        <div style={{ fontSize: 11, color: 'var(--feftms-text-muted)', marginTop: 4 }}>
                          Driver: {d.order.driverName} ({d.order.driverPhone})
                        </div>
                      )}
                      {d.order?.locationAddress && (
                        <div style={{ fontSize: 10, color: 'var(--feftms-text-muted)', marginTop: 2 }} title={`GPS: ${d.order.locationGps || 'N/A'} | Landmark: ${d.order.locationLandmark || 'N/A'}`}>
                          📍 {d.order.locationAddress}
                        </div>
                      )}
                    </td>
                    <td>{d.quantity} L</td>
                    <td>
                      <span className={`fef-badge fef-badge-${d.deliveryStatus?.toLowerCase()}`}>
                        {d.deliveryStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* DRIVERS */}
      {activeTab === "drivers" && (
        <div className="fef-panel">
          <div className="fef-panel-head" style={{ marginBottom: 20 }}>
            <h3>Active Operational Drivers</h3>
            <button className="fef-btn fef-btn-primary" onClick={() => setShowDriverModal(true)}>
              <FiPlus /> Register Driver
            </button>
          </div>

          {showDriverModal && (
            <div className="fef-card" style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}>
              <h4>Register Operational Driver</h4>
              <form onSubmit={handleCreateDriver} style={{ marginTop: 15 }}>
                <div className="fef-form-grid">
                  <div className="fef-field">
                    <label className="fef-label">First Name</label>
                    <input required className="fef-input" value={driverForm.firstName} onChange={(e) => setDriverForm({ ...driverForm, firstName: e.target.value })} placeholder="Peter" />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Last Name</label>
                    <input required className="fef-input" value={driverForm.lastName} onChange={(e) => setDriverForm({ ...driverForm, lastName: e.target.value })} placeholder="Maina" />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Phone Number</label>
                    <input className="fef-input" value={driverForm.phone} onChange={(e) => setDriverForm({ ...driverForm, phone: e.target.value })} placeholder="+254 711 000000" />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Driving License Number</label>
                    <input required className="fef-input" value={driverForm.licenseNumber} onChange={(e) => setDriverForm({ ...driverForm, licenseNumber: e.target.value })} placeholder="DL-123456" />
                  </div>
                </div>
                <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                  <button type="submit" className="fef-btn fef-btn-primary">Register</button>
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowDriverModal(false)}>Cancel</button>
                </div>
              </form>
            </div>
          )}

          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Phone</th>
                  <th>License Number</th>
                  <th>Duty Status</th>
                </tr>
              </thead>
              <tbody>
                {driversList.map((d) => (
                  <tr key={d.id}>
                    <td>{d.firstName} {d.lastName}</td>
                    <td>{d.phone || "—"}</td>
                    <td>{d.licenseNumber}</td>
                    <td>
                      <span className={`fef-badge fef-badge-${d.status === "AVAILABLE" ? "approved" : "progress"}`}>
                        {d.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* VEHICLES */}
      {activeTab === "vehicles" && (
        <div className="fef-panel">
          <div className="fef-panel-head" style={{ marginBottom: 20 }}>
            <h3>Fleet Tankers</h3>
            <button className="fef-btn fef-btn-primary" onClick={() => setShowVehicleModal(true)}>
              <FiPlus /> Register Vehicle
            </button>
          </div>

          {showVehicleModal && (
            <div className="fef-card" style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}>
              <h4>Register Fleet Vehicle</h4>
              <form onSubmit={handleCreateVehicle} style={{ marginTop: 15 }}>
                <div className="fef-form-grid">
                  <div className="fef-field">
                    <label className="fef-label">Plate Number</label>
                    <input required className="fef-input" value={vehicleForm.plateNumber} onChange={(e) => setVehicleForm({ ...vehicleForm, plateNumber: e.target.value })} placeholder="KDA 432X" />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Max Capacity (Litres)</label>
                    <input required type="number" className="fef-input" value={vehicleForm.capacity} onChange={(e) => setVehicleForm({ ...vehicleForm, capacity: e.target.value })} placeholder="35000" />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Link Driver (Optional)</label>
                    <select className="fef-select" value={vehicleForm.driverId} onChange={(e) => setVehicleForm({ ...vehicleForm, driverId: e.target.value })}>
                      <option value="">-- No Assigned Driver --</option>
                      {driversList.map(d => (
                        <option key={d.id} value={d.id}>{d.firstName} {d.lastName}</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                  <button type="submit" className="fef-btn fef-btn-primary">Register</button>
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowVehicleModal(false)}>Cancel</button>
                </div>
              </form>
            </div>
          )}

          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Plate Number</th>
                  <th>Capacity</th>
                  <th>Assigned Driver</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {vehiclesList.map((v) => (
                  <tr key={v.id}>
                    <td><strong>{v.plateNumber}</strong></td>
                    <td>{v.capacity?.toLocaleString()} L</td>
                    <td>{v.driverName || "—"}</td>
                    <td>
                      <span className="fef-badge fef-badge-approved">
                        {v.currentStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* INVENTORY CONTROL */}
      {activeTab === "inventory" && (
        <div className="fef-panel">
          <div className="fef-panel-head" style={{ marginBottom: 20 }}>
            <h3>Inventory & Stock Control</h3>
            <button className="fef-btn fef-btn-primary" onClick={() => setShowCreateProductModal(true)}>
              <FiPlus /> Add New Fuel Product
            </button>
          </div>

          {showCreateProductModal && (
            <div className="fef-modal-overlay" onClick={() => setShowCreateProductModal(false)}>
              <div className="fef-modal" onClick={(e) => e.stopPropagation()}>
                <h2 className="fef-detail-modal-title">Create New Fuel Product</h2>
                <form onSubmit={handleCreateProduct} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  <div className="fef-field">
                    <label className="fef-label">Product Name</label>
                    <input 
                      required 
                      type="text" 
                      className="fef-input" 
                      value={createProductForm.productName} 
                      onChange={(e) => setCreateProductForm({ ...createProductForm, productName: e.target.value })} 
                      placeholder="e.g. Petrol, PMS, AGO, Diesel"
                    />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Fuel Type</label>
                    <input 
                      required 
                      type="text" 
                      className="fef-input" 
                      value={createProductForm.fuelType} 
                      onChange={(e) => setCreateProductForm({ ...createProductForm, fuelType: e.target.value })} 
                      placeholder="e.g. Petrol, Diesel, Kerosene"
                    />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Density (kg/L)</label>
                    <input 
                      required 
                      type="number" 
                      step="0.01"
                      className="fef-input" 
                      value={createProductForm.density} 
                      onChange={(e) => setCreateProductForm({ ...createProductForm, density: e.target.value })} 
                      placeholder="0.74"
                    />
                  </div>
                  <div className="fef-field">
                    <label className="fef-label">Initial Stock (Litres)</label>
                    <input 
                      required 
                      type="number" 
                      className="fef-input" 
                      value={createProductForm.availableQuantity} 
                      onChange={(e) => setCreateProductForm({ ...createProductForm, availableQuantity: e.target.value })} 
                      placeholder="0"
                    />
                  </div>
                  <p style={{ fontSize: 12, color: "var(--feftms-text-muted)", marginBottom: 0 }}>
                    Note: Finance will set the unit price after this product is created.
                  </p>
                  <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                    <button type="submit" className="fef-btn fef-btn-primary">Create Product</button>
                    <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowCreateProductModal(false)}>Cancel</button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {tanksList.length > 0 && (
            <div style={{ marginBottom: 18 }}>
              <h4 style={{ margin: "8px 0" }}>Storage Tanks</h4>
              <div className="fef-table-wrap">
                <table className="fef-table">
                  <thead>
                    <tr>
                      <th>Tank</th>
                      <th>Capacity (L)</th>
                      <th>Current Volume (L)</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tanksList.map((t) => (
                      <tr key={t.id}>
                        <td>{t.tankName || t.id}</td>
                        <td>{t.capacity?.toLocaleString() || "—"}</td>
                        <td style={{ fontWeight: 700 }}>{t.currentVolume?.toLocaleString() || 0} L</td>
                        <td><span className={`fef-badge fef-badge-${(t.status || '').toLowerCase() === 'active' ? 'success' : 'danger'}`}>{t.status}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Product Name</th>
                  <th>Fuel Type</th>
                  <th>Unit Price (Read-only)</th>
                  <th>Density</th>
                  <th>Available Stock (Litres)</th>
                  <th>Availability Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {productsList.length === 0 ? (
                  <tr>
                    <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                      No fuel products yet. Click "Add New Fuel Product" to create one.
                    </td>
                  </tr>
                ) : (
                  productsList.map((p) => (
                  <tr key={p.id}>
                    <td><strong>{p.productName}</strong></td>
                    <td>{p.fuelType}</td>
                    <td style={{ color: "var(--feftms-text-muted)" }}>${p.unitPrice?.toFixed(2)}/L</td>
                    <td>{p.density} kg/L</td>
                    <td style={{ fontWeight: 700, color: p.availableQuantity > 0 ? "var(--feftms-success)" : "var(--feftms-danger)" }}>
                      {p.availableQuantity?.toLocaleString()} L
                    </td>
                    <td>
                      <span className={`fef-badge fef-badge-${p.status?.toLowerCase() === 'active' || p.status?.toLowerCase() === 'available' ? 'success' : 'danger'}`}>
                        {p.status}
                      </span>
                    </td>
                    <td style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                      <button 
                        className="fef-btn fef-btn-outline" 
                        style={{ padding: "6px 12px", fontSize: "13px" }}
                        onClick={() => handleOpenStockModal(p)}
                      >
                        Adjust Stock / Status
                      </button>
                      <button
                        className="fef-btn fef-btn-danger"
                        style={{ padding: "6px 12px", fontSize: "13px" }}
                        onClick={() => handleDeleteProduct(p)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* MODAL: STOCK UPDATE */}
      {showStockModal && selectedProduct && (
        <div className="fef-modal-backdrop" onClick={() => setShowStockModal(false)}>
          <div className="fef-modal-window" style={{ maxWidth: "500px" }} onClick={(e) => e.stopPropagation()}>
            <button className="fef-modal-close" onClick={() => setShowStockModal(false)}>
              <FiX />
            </button>
            <div className="fef-detail-modal-header">
              <h2 className="fef-detail-modal-title">Inventory Adjustment</h2>
              <p style={{ margin: "4px 0 0", color: "var(--feftms-text-muted)" }}>
                Update available stock for {selectedProduct.productName} (Current: {selectedProduct.availableQuantity?.toLocaleString()} L)
              </p>
            </div>
            <form onSubmit={handleStockSubmit} style={{ marginTop: 20 }}>
              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                
                <div className="fef-field">
                  <label className="fef-label">Adjustment Type</label>
                  <select 
                    className="fef-select"
                    value={stockForm.actionType}
                    onChange={(e) => setStockForm({ ...stockForm, actionType: e.target.value })}
                  >
                    <option value="ADD">Add Received Fuel Delivery (Increase Stock)</option>
                    <option value="SUBTRACT">Inventory Adjustment / Shrinkage (Decrease Stock)</option>
                    <option value="STATUS_ONLY">Manual Availability Status Override Only</option>
                  </select>
                </div>

                {stockForm.actionType !== "STATUS_ONLY" && (
                  <div className="fef-field">
                    <label className="fef-label">Quantity (Litres)</label>
                    <input 
                      required
                      type="number"
                      min="1"
                      className="fef-input"
                      value={stockForm.quantity}
                      onChange={(e) => setStockForm({ ...stockForm, quantity: e.target.value })}
                      placeholder="e.g. 5000"
                    />
                  </div>
                )}

                <div className="fef-field">
                  <label className="fef-label">Status Override / Manual Control</label>
                  <select 
                    className="fef-select"
                    value={stockForm.overrideStatus}
                    onChange={(e) => setStockForm({ ...stockForm, overrideStatus: e.target.value })}
                  >
                    <option value="">-- Auto Determine (Active if stock &gt; 0) --</option>
                    <option value="ACTIVE">FORCE AVAILABLE (ACTIVE)</option>
                    <option value="UNAVAILABLE">FORCE UNAVAILABLE (Tank maintenance, contamination, etc.)</option>
                  </select>
                </div>

                <div className="fef-field" style={{ opacity: 0.6 }}>
                  <label className="fef-label">Selling Price per Litre (Locked — Finance Only)</label>
                  <input 
                    disabled
                    type="text"
                    className="fef-input"
                    value={`$${selectedProduct.unitPrice?.toFixed(2)}/L (Cannot modify)`}
                  />
                </div>
              </div>
              
              <div className="fef-detail-actions" style={{ marginTop: 24 }}>
                <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowStockModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="fef-btn fef-btn-primary">
                  <FiCheck style={{ marginRight: 4 }} /> Save Inventory Updates
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      </DashboardLayout>
    </RouteGuard>
  );
}
