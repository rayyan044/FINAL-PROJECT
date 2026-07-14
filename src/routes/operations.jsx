import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
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
  FiActivity,
  FiPrinter,
  FiInfo,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { listRequests } from "../services/requestService";
import { listDeliveries, createDelivery } from "../services/deliveryService";
import { listDrivers, createDriver } from "../services/driverService";
import { listVehicles, createVehicle } from "../services/vehicleService";
import {
  listProducts,
  updateProduct,
  createProduct,
  deleteProduct,
} from "../services/productService";
import { listTanks } from "../services/tankService";
import {
  createLoadingOrder,
  updateLoadingOrder,
  getLoadingOrderById,
  getLoadingOrderByOrderId,
  listLoadingOrders,
  approveLoadingOrder,
  cancelLoadingOrder,
  startLoadingActivity,
  completeLoadingActivity,
} from "../services/loadingOrderService";
import { rejectNomination, approveNomination, getNominationByOrderId } from "../services/truckNominationService";
import "../styles/forms.css";
import falconLogo from "../assets/falcon-logo.png";

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
  const [stats, setStats] = useState({
    loadingOrders: 0,
    waitingDrivers: 0,
    deliveries: 0,
    vehicles: 0,
  });
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
  const [driverForm, setDriverForm] = useState({
    firstName: "",
    lastName: "",
    phone: "",
    licenseNumber: "",
  });

  const [showVehicleModal, setShowVehicleModal] = useState(false);
  const [vehicleForm, setVehicleForm] = useState({ plateNumber: "", capacity: "", driverId: "" });

  const [showStockModal, setShowStockModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [stockForm, setStockForm] = useState({
    actionType: "ADD", // ADD, SUBTRACT, STATUS_ONLY
    quantity: "",
    overrideStatus: "",
  });

  const [showCreateProductModal, setShowCreateProductModal] = useState(false);
  const [createProductForm, setCreateProductForm] = useState({
    productName: "",
    fuelType: "",
    density: "",
    availableQuantity: "0",
  });

  // Loading Order & Activities States
  const [loadingOrdersList, setLoadingOrdersList] = useState([]);
  const [selectedLoadingOrder, setSelectedLoadingOrder] = useState(null);

  const handlePrintLO = () => {
    const printContent = document.getElementById("printable-loading-order");
    if (!printContent || !selectedLoadingOrder) return;

    const win = window.open("", "_blank");
    win.document.write(`
      <html>
        <head>
          <title>Loading Order - ${selectedLoadingOrder.loadingOrderNumber}</title>
          <style>
            body {
              font-family: Arial, sans-serif;
              padding: 20px;
              background: #fff;
              color: #000;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin-bottom: 20px;
            }
            th, td {
              border-bottom: 1px solid #000;
              padding: 8px;
              text-align: left;
            }
            .fef-badge {
              display: inline-block;
              padding: 3px 8px;
              font-size: 11px;
              font-weight: bold;
              border-radius: 4px;
              text-transform: uppercase;
              border: 1px solid #000;
            }
            
            /* Print adjustments */
            @media print {
              body {
                padding: 0;
              }
              @page {
                size: A4;
                margin: 20mm;
              }
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
  const [showCreateLOForm, setShowCreateLOForm] = useState(false);
  const [selectedOrderForLO, setSelectedOrderForLO] = useState(null);
  const [loForm, setLoForm] = useState({
    loadingTerminal: "",
    loadingDate: new Date().toISOString().split("T")[0],
    consignee: "",
    loadingRemarks: "",
  });
  const [showRejectNominationModal, setShowRejectNominationModal] = useState(false);
  const [selectedOrderForRejection, setSelectedOrderForRejection] = useState(null);
  const [rejectionReason, setRejectionReason] = useState("");

  const [showReviewNominationModal, setShowReviewNominationModal] = useState(false);
  const [selectedOrderForReview, setSelectedOrderForReview] = useState(null);
  const [nominationDetails, setNominationDetails] = useState(null);
  const [nominationDetailsLoading, setNominationDetailsLoading] = useState(false);
  const [submittingNomination, setSubmittingNomination] = useState(false);

  const [showEditLOForm, setShowEditLOForm] = useState(false);
  const [editingLO, setEditingLO] = useState(null);
  const [editLOForm, setEditLOForm] = useState({
    loadingTerminal: "",
    loadingDate: "",
    consignee: "",
    loadingRemarks: "",
    activities: [],
  });

  const [activityInputs, setActivityInputs] = useState({});

  const handleActivityInputChange = (actId, field, value) => {
    setActivityInputs(prev => ({
      ...prev,
      [actId]: {
        ...prev[actId],
        [field]: value
      }
    }));
  };

  const handleOpenCreateLO = (order) => {
    setSelectedOrderForLO(order);
    const companyName = (order.customerName === "Stranded Drivers (Emergency Requests)" || order.customerName === "Customer Fuel Requests") && order.driverName
      ? order.driverName
      : (order.customerName || "");
    setLoForm({
      loadingTerminal: "",
      loadingDate: new Date().toISOString().split("T")[0],
      consignee: companyName,
      loadingRemarks: "",
      vesselName: "TBA",
      operationsManager: "GSG ENERGIES\nDAR ES SALAAM",
    });
    setShowCreateLOForm(true);
  };

  const handleCreateLOSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const payload = {
        orderId: selectedOrderForLO.id,
        loadingDate: loForm.loadingDate,
        loadingTerminal: loForm.loadingTerminal,
        consignee: loForm.consignee,
        loadingRemarks: loForm.loadingRemarks,
        vesselName: loForm.vesselName,
        operationsManager: loForm.operationsManager,
      };
      await createLoadingOrder(payload);
      setSuccess("Loading Order created successfully.");
      setShowCreateLOForm(false);
      setSelectedOrderForLO(null);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to create Loading Order.");
    }
  };

  const handleApproveLO = async (id) => {
    setError("");
    setSuccess("");
    try {
      const res = await approveLoadingOrder(id);
      const updated = res.data || res;
      setSuccess(`Loading Order ${updated.loadingOrderNumber} approved and locked.`);
      if (selectedLoadingOrder && selectedLoadingOrder.id === id) {
        setSelectedLoadingOrder(updated);
      }
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to approve Loading Order.");
    }
  };

  const handleCancelLO = async (id) => {
    if (!window.confirm("Are you sure you want to cancel this Loading Order?")) return;
    setError("");
    setSuccess("");
    try {
      const res = await cancelLoadingOrder(id);
      const updated = res.data || res;
      setSuccess(`Loading Order ${updated.loadingOrderNumber} has been cancelled.`);
      if (selectedLoadingOrder && selectedLoadingOrder.id === id) {
        setSelectedLoadingOrder(updated);
      }
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to cancel Loading Order.");
    }
  };

  const handleStartLoading = async (loId, activityId) => {
    setError("");
    setSuccess("");
    try {
      const inputs = activityInputs[activityId] || {};
      const params = {
        bayNumber: inputs.bayNumber || "BAY-1",
        pumpNumber: inputs.pumpNumber || ""
      };
      const res = await startLoadingActivity(loId, activityId, params);
      const updated = res.data || res;
      setSuccess("Loading started for truck.");
      setSelectedLoadingOrder(updated);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to start loading.");
    }
  };

  const handleCompleteLoading = async (loId, activityId) => {
    setError("");
    setSuccess("");
    try {
      const res = await completeLoadingActivity(loId, activityId);
      const updated = res.data || res;
      setSuccess("Loading completed for truck.");
      setSelectedLoadingOrder(updated);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to complete loading.");
    }
  };

  const handleOpenRejectNomination = (order) => {
    setSelectedOrderForRejection(order);
    setRejectionReason("");
    setShowRejectNominationModal(true);
  };

  const handleRejectNominationSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      // Find nomination by orderId
      // We will reject nomination using the order's nomination
      // First we need to get the nomination id or we can query it
      const nomRes = await getNominationByOrderId(selectedOrderForRejection.id);
      const nom = nomRes.data || nomRes;
      await rejectNomination(nom.id, rejectionReason);
      setSuccess("Truck nomination changes requested. Returned to DRAFT status for Sales Officer.");
      setShowRejectNominationModal(false);
      setSelectedOrderForRejection(null);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to request changes on truck nomination.");
    }
  };
  const handleOpenReviewNomination = async (order) => {
    setSelectedOrderForReview(order);
    setShowReviewNominationModal(true);
    setNominationDetailsLoading(true);
    setNominationDetails(null);
    setError("");
    setSuccess("");
    try {
      const nomRes = await getNominationByOrderId(order.id);
      const nom = nomRes.data || nomRes;
      setNominationDetails(nom);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to retrieve nomination details.");
    } finally {
      setNominationDetailsLoading(false);
    }
  };

  const handleApproveNomination = async () => {
    if (!nominationDetails) return;
    setSubmittingNomination(true);
    setError("");
    setSuccess("");
    try {
      await approveNomination(nominationDetails.id);
      setSuccess("Truck nomination approved successfully.");
      setShowReviewNominationModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to approve nomination.");
    } finally {
      setSubmittingNomination(false);
    }
  };

  const handleOpenEditLO = (lo) => {
    setEditingLO(lo);
    setEditLOForm({
      loadingTerminal: lo.loadingTerminal || "",
      loadingDate: lo.loadingDate || "",
      consignee: lo.consignee || "",
      loadingRemarks: lo.loadingRemarks || "",
      vesselName: lo.vesselName || "",
      operationsManager: lo.operationsManager || "",
      activities: lo.activities ? lo.activities.map(act => ({
        id: act.id,
        truckNumber: act.truckNumber || "",
        trailerNumber: act.trailerNumber || "",
        driverName: act.driverName || "",
        driverLicenceNumber: act.driverLicenceNumber || "",
        driverPassport: act.driverPassport || "",
        transportCompany: act.transportCompany || "",
        destination: act.destination || "",
        allocatedQuantity: act.allocatedQuantity || 0,
      })) : [],
    });
    setShowEditLOForm(true);
  };

  const handleEditLOAddTruck = () => {
    setEditLOForm(prev => ({
      ...prev,
      activities: [
        ...prev.activities,
        {
          truckNumber: "",
          trailerNumber: "",
          driverName: "",
          driverLicenceNumber: "",
          driverPassport: "",
          transportCompany: "",
          destination: "",
          allocatedQuantity: 0,
        }
      ]
    }));
  };

  const handleEditLORemoveTruck = (idx) => {
    setEditLOForm(prev => ({
      ...prev,
      activities: prev.activities.filter((_, i) => i !== idx)
    }));
  };

  const handleEditLOTruckFieldChange = (idx, field, value) => {
    setEditLOForm(prev => {
      const newActs = [...prev.activities];
      newActs[idx] = { ...newActs[idx], [field]: value };
      return { ...prev, activities: newActs };
    });
  };

  const handleSaveEditLO = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    
    const totalAllocated = editLOForm.activities.reduce((sum, act) => sum + parseFloat(act.allocatedQuantity || 0), 0);
    const orderQty = editingLO.approvedQuantity || 0;
    if (totalAllocated > orderQty) {
      setError(`Total allocated quantity (${totalAllocated} L) cannot exceed approved customer order quantity (${orderQty} L).`);
      return;
    }
    
    if (editLOForm.activities.length === 0) {
      setError("At least one truck must be listed in the Loading Order.");
      return;
    }

    try {
      const payload = {
        orderId: editingLO.orderId,
        loadingTerminal: editLOForm.loadingTerminal,
        loadingDate: editLOForm.loadingDate,
        consignee: editLOForm.consignee,
        loadingRemarks: editLOForm.loadingRemarks,
        vesselName: editLOForm.vesselName,
        operationsManager: editLOForm.operationsManager,
        activities: editLOForm.activities.map(act => ({
          id: act.id,
          truckNumber: act.truckNumber,
          trailerNumber: act.trailerNumber,
          driverName: act.driverName,
          driverLicenceNumber: act.driverLicenceNumber,
          driverPassport: act.driverPassport,
          transportCompany: act.transportCompany,
          destination: act.destination,
          allocatedQuantity: parseFloat(act.allocatedQuantity || 0),
        })),
      };

      const res = await updateLoadingOrder(editingLO.id, payload);
      setSuccess("Loading Order updated successfully.");
      setShowEditLOForm(false);
      setSelectedLoadingOrder(res.data || res);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to update Loading Order.");
    }
  };
  const loadData = () => {
    setLoading(true);
    setError("");

    Promise.allSettled([
      listRequests({ size: 100 }),
      listDeliveries({ size: 100 }),
      listDrivers({ size: 100 }),
      listVehicles({ size: 100 }),
      listProducts({ size: 100 }),
      listTanks({ size: 100 }),
      listLoadingOrders(),
    ])
      .then(([reqsRes, delsRes, drvsRes, vehsRes, prodsRes, tanksRes, loRes]) => {
        const failures = [];

        if (reqsRes.status === "fulfilled") {
          const reqs = reqsRes.value;
          const approvedOrders = (reqs.content || []).filter((o) => o.orderStatus === "APPROVED");
          setOrdersList(reqs.content || []);
        } else {
          failures.push({ name: "requests", error: reqsRes.reason });
          setOrdersList([]);
        }

        if (delsRes.status === "fulfilled") {
          const dels = delsRes.value;
          setStats((prev) => ({
            ...prev,
            deliveries: dels.totalElements || 0,
          }));
          setDeliveriesList(dels.content || []);
        } else {
          failures.push({ name: "deliveries", error: delsRes.reason });
          setDeliveriesList([]);
        }

        if (drvsRes.status === "fulfilled") {
          const drvs = drvsRes.value;
          const availableDrivers = (drvs.content || []).filter((d) => d.status === "AVAILABLE");
          setStats((prev) => ({
            ...prev,
            waitingDrivers: availableDrivers.length,
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
            vehicles: vehs.totalElements || 0,
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

        if (loRes.status === "fulfilled") {
          const los = loRes.value.data || loRes.value || [];
          setLoadingOrdersList(los);
          setStats((prev) => ({
            ...prev,
            loadingOrders: los.length,
          }));
        } else {
          failures.push({ name: "loadingOrders", error: loRes.reason });
          setLoadingOrdersList([]);
        }

        if (failures.length > 0) {
          const failureNames = failures.map((failure) => failure.name).join(", ");
          setError(
            `Partial data load failed for: ${failureNames}. Some panels may be unavailable.`,
          );
          failures.forEach((failure) =>
            console.warn(`Failed to load ${failure.name}:`, failure.error),
          );
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
        deliveryStatus: "PENDING",
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
        currentStatus: "ACTIVE",
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
      overrideStatus: prod.status,
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
        status: stockForm.overrideStatus || (finalQty > 0 ? "ACTIVE" : "UNAVAILABLE"),
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
        status: qty > 0 ? "ACTIVE" : "UNAVAILABLE",
      });

      setSuccess(
        `Fuel product "${createProductForm.productName}" created successfully. Finance can now set the price.`,
      );
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
        <PageHeader
          title={
            activeTab === "dash"
              ? "Operations Overview"
              : activeTab === "inventory"
                ? "Inventory Control"
                : activeTab === "loading"
                  ? "Approved Loading Orders"
                  : activeTab === "deliveries"
                    ? "Active Deliveries"
                    : activeTab === "drivers"
                      ? "Drivers Directory"
                      : "Vehicles Fleet"
          }
          crumbs={["Operations", activeTab]}
        />

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
              <StatCard
                label="Approved Loading Orders"
                value={stats.loadingOrders}
                icon={FiPackage}
                tone="primary"
              />
              <StatCard
                label="Available Drivers"
                value={stats.waitingDrivers}
                icon={FiUserPlus}
                tone="accent"
              />
              <StatCard
                label="Total Deliveries"
                value={stats.deliveries}
                icon={FiNavigation}
                tone="secondary"
              />
              <StatCard
                label="Fleet Vehicles"
                value={stats.vehicles}
                icon={FiTruck}
                tone="success"
              />
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
                          <span
                            className={`fef-badge fef-badge-${d.deliveryStatus?.toLowerCase()}`}
                          >
                            {d.deliveryStatus}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {deliveriesList.length === 0 && (
                      <tr>
                        <td
                          colSpan="5"
                          style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                        >
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
            {/* Create Loading Order Modal */}
            {showCreateLOForm &&
              selectedOrderForLO &&
              typeof window !== "undefined" &&
              createPortal(
                <div className="fef-modal-backdrop" onClick={() => {
                  setShowCreateLOForm(false);
                  setSelectedOrderForLO(null);
                }}>
                  <div className="fef-modal-window" style={{ maxWidth: "850px" }} onClick={(e) => e.stopPropagation()}>
                    <button
                      className="fef-modal-close"
                      onClick={() => {
                        setShowCreateLOForm(false);
                        setSelectedOrderForLO(null);
                      }}
                    >
                      <FiX />
                    </button>
                    
                    <div className="fef-detail-modal-header">
                      <div className="fef-badge-row">
                        <span className="fef-badge fef-badge-pending" style={{ fontSize: "11px" }}>
                          {selectedOrderForLO.orderNumber}
                        </span>
                        <span className={`fef-badge fef-badge-${selectedOrderForLO.orderStatus?.toLowerCase()}`}>
                          {selectedOrderForLO.orderStatus}
                        </span>
                        <span className={`fef-badge fef-badge-${selectedOrderForLO.truckNominationStatus === "APPROVED" ? "success" : "warning"}`} style={{ fontSize: "11px" }}>
                          Nomination: {selectedOrderForLO.truckNominationStatus || "PENDING"}
                        </span>
                      </div>
                      <h2 className="fef-detail-modal-title">Create Loading Order</h2>
                    </div>

                    <form onSubmit={handleCreateLOSubmit}>
                      <div className="fef-detail-modal-body" style={{ maxHeight: "60vh", overflowY: "auto", marginTop: 15 }}>
                        <div style={{ display: "flex", flexDirection: "column", gap: 15 }}>
                          <div className="fef-field">
                            <label className="fef-label">Loading Terminal</label>
                            <input
                              required
                              className="fef-input"
                              placeholder="e.g. Dar es Salaam Terminal 1"
                              value={loForm.loadingTerminal}
                              onChange={(e) => setLoForm({ ...loForm, loadingTerminal: e.target.value })}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Loading Date</label>
                            <input
                              type="date"
                              required
                              className="fef-input"
                              value={loForm.loadingDate}
                              onChange={(e) => setLoForm({ ...loForm, loadingDate: e.target.value })}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Consignee</label>
                            <input
                              readOnly
                              className="fef-input"
                              style={{ backgroundColor: "#f3f4f6", cursor: "not-allowed" }}
                              value={loForm.consignee}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Vessel Name</label>
                            <input
                              required
                              className="fef-input"
                              placeholder="e.g. TBA"
                              value={loForm.vesselName || ""}
                              onChange={(e) => setLoForm({ ...loForm, vesselName: e.target.value })}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Operations Manager / Address Info</label>
                            <textarea
                              required
                              className="fef-input"
                              rows="2"
                              placeholder="e.g. GSG ENERGIES&#10;DAR ES SALAAM"
                              value={loForm.operationsManager || ""}
                              onChange={(e) => setLoForm({ ...loForm, operationsManager: e.target.value })}
                            />
                          </div>
                        </div>
                      </div>
                      
                      <div className="fef-detail-modal-footer" style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 20 }}>
                        <button
                          type="button"
                          className="fef-btn fef-btn-outline"
                          onClick={() => {
                            setShowCreateLOForm(false);
                            setSelectedOrderForLO(null);
                          }}
                        >
                          Cancel
                        </button>
                        <button type="submit" className="fef-btn fef-btn-primary">
                          Generate Loading Order
                        </button>
                      </div>
                    </form>
                  </div>
                </div>,
                document.body
              )
            }

            {/* Request Changes Modal */}
            {showRejectNominationModal && selectedOrderForRejection && (
              <div className="fef-card" style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}>
                <h4>Request Truck Nomination Changes for Order {selectedOrderForRejection.orderNumber}</h4>
                <form onSubmit={handleRejectNominationSubmit} style={{ marginTop: 15 }}>
                  <div className="fef-field">
                    <label className="fef-label">Specify Required Corrections / Remarks</label>
                    <textarea
                      required
                      className="fef-input"
                      rows="3"
                      placeholder="Specify why changes are requested (e.g. Invalid driver licence, allocated qty mismatch, etc.)"
                      value={rejectionReason}
                      onChange={(e) => setRejectionReason(e.target.value)}
                    />
                  </div>
                  <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                    <button type="submit" className="fef-btn fef-btn-danger">
                      Request Revisions
                    </button>
                    <button
                      type="button"
                      className="fef-btn fef-btn-outline"
                      onClick={() => {
                        setShowRejectNominationModal(false);
                        setSelectedOrderForRejection(null);
                      }}
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Review Nomination Popup Modal */}
            {showReviewNominationModal &&
              selectedOrderForReview &&
              typeof window !== "undefined" &&
              createPortal(
                <div className="fef-modal-backdrop" onClick={() => setShowReviewNominationModal(false)}>
                  <div className="fef-modal-window" style={{ maxWidth: "850px" }} onClick={(e) => e.stopPropagation()}>
                    <button className="fef-modal-close" onClick={() => setShowReviewNominationModal(false)}>
                      <FiX />
                    </button>
                    
                    <div className="fef-detail-modal-header">
                      <div className="fef-badge-row">
                        <span className="fef-badge fef-badge-pending" style={{ fontSize: "11px" }}>
                          {selectedOrderForReview.orderNumber}
                        </span>
                        <span className={`fef-badge fef-badge-${selectedOrderForReview.orderStatus?.toLowerCase()}`}>
                          {selectedOrderForReview.orderStatus}
                        </span>
                        <span className={`fef-badge fef-badge-${selectedOrderForReview.truckNominationStatus === "APPROVED" ? "success" : "warning"}`} style={{ fontSize: "11px" }}>
                          Nomination: {selectedOrderForReview.truckNominationStatus || "PENDING"}
                        </span>
                      </div>
                      <h2 className="fef-detail-modal-title">Truck Nomination Details</h2>
                    </div>

                    <div className="fef-detail-modal-body" style={{ maxHeight: "60vh", overflowY: "auto" }}>
                      {error && (
                        <div className="fef-alert fef-alert-danger" style={{ marginBottom: 15 }}>
                          <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
                          {error}
                        </div>
                      )}
                      {nominationDetailsLoading && <p>Loading nomination details...</p>}
                      {!nominationDetailsLoading && !nominationDetails && <p style={{ color: "#EF4444" }}>Could not load nomination details.</p>}
                      {!nominationDetailsLoading && nominationDetails && (
                        <div>
                          {/* Summary Section */}
                          <div className="fef-detail-section">
                            <h4 className="fef-detail-section-title">Transport Summary</h4>
                            <div className="fef-detail-grid" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))" }}>
                              <div className="fef-detail-item">
                                <span className="fef-detail-label">Customer Transport</span>
                                <span className="fef-detail-value">
                                  {nominationDetails.transportSource === "CUSTOMER_TRUCKS" ? "Yes (Customer Own)" : "No (Falcon Arranged)"}
                                </span>
                              </div>
                              {nominationDetails.transportSource === "CUSTOMER_TRUCKS" && (
                                <div className="fef-detail-item">
                                  <span className="fef-detail-label">Number of Trucks</span>
                                  <span className="fef-detail-value">{nominationDetails.numberOfTrucks}</span>
                                </div>
                              )}
                              <div className="fef-detail-item">
                                <span className="fef-detail-label">Total Allocated Quantity</span>
                                <span className="fef-detail-value">{nominationDetails.totalAllocatedQuantity?.toLocaleString()} L</span>
                              </div>
                            </div>

                            {nominationDetails.confirmationNotes && (
                              <div style={{ marginTop: 15 }}>
                                <span className="fef-detail-label">Logistics Notes</span>
                                <p className="fef-detail-value" style={{ margin: "5px 0 0 0", fontSize: 13, background: "rgba(255,255,255,0.02)", padding: 10, borderRadius: 4 }}>
                                  {nominationDetails.confirmationNotes}
                                </p>
                              </div>
                            )}
                          </div>

                          {/* Trucks Section */}
                          <div className="fef-detail-section" style={{ marginTop: 25 }}>
                            <h4 className="fef-detail-section-title">Nominated Trucks</h4>
                            <div className="fef-table-wrap" style={{ marginTop: 10 }}>
                              <table className="fef-table" style={{ fontSize: 12 }}>
                                <thead>
                                  <tr>
                                    <th>Truck No</th>
                                    <th>Trailer No</th>
                                    <th>Driver Name</th>
                                    <th>Licence</th>
                                    <th>Passport/ID</th>
                                    <th>Transporter</th>
                                    <th>Destination</th>
                                    <th>Capacity</th>
                                    <th>Allocated</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {nominationDetails.items?.map((item, idx) => (
                                    <tr key={idx}>
                                      <td><strong>{item.truckNumber}</strong></td>
                                      <td>{item.trailerNumber || "N/A"}</td>
                                      <td>{item.driverName}</td>
                                      <td>{item.driverLicenceNumber}</td>
                                      <td>{item.driverPassport || "N/A"}</td>
                                      <td>{item.transportCompany}</td>
                                      <td>{item.destination}</td>
                                      <td>{item.truckCapacity?.toLocaleString()} L</td>
                                      <td><strong>{item.allocatedQuantity?.toLocaleString()} L</strong></td>
                                    </tr>
                                  ))}
                                  {(!nominationDetails.items || nominationDetails.items.length === 0) && (
                                    <tr>
                                      <td colSpan="9" style={{ textAlign: "center", padding: 20, color: "var(--feftms-text-muted)" }}>
                                        No nominated trucks. Allocation will be handled during Loading Order drafting.
                                      </td>
                                    </tr>
                                  )}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="fef-detail-modal-footer" style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
                      <button
                        type="button"
                        className="fef-btn fef-btn-outline fef-detail-close-btn"
                        onClick={() => setShowReviewNominationModal(false)}
                        style={{ margin: 0 }}
                      >
                        Close
                      </button>
                      {!nominationDetailsLoading && nominationDetails && (
                        <>
                          <button
                            type="button"
                            className="fef-btn fef-btn-outline"
                            onClick={() => {
                              setShowReviewNominationModal(false);
                              handleOpenRejectNomination(selectedOrderForReview);
                            }}
                            disabled={submittingNomination}
                            style={{ color: "#EF4444", borderColor: "rgba(239, 68, 68, 0.4)", margin: 0 }}
                          >
                            Request Changes
                          </button>
                          <button
                            type="button"
                            className="fef-btn fef-btn-primary"
                            onClick={handleApproveNomination}
                            disabled={submittingNomination}
                            style={{ background: "#10B981", margin: 0 }}
                          >
                            {submittingNomination ? "Approving..." : "Approve Nomination"}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>,
                document.body
              )
            }

            {/* Edit Loading Order Card */}
            {showEditLOForm &&
              editingLO &&
              typeof window !== "undefined" &&
              createPortal(
                <div className="fef-modal-backdrop" onClick={() => setShowEditLOForm(false)}>
                  <div className="fef-modal-window" style={{ maxWidth: "850px" }} onClick={(e) => e.stopPropagation()}>
                    <button className="fef-modal-close" onClick={() => setShowEditLOForm(false)}>
                      <FiX />
                    </button>
                    
                    <div className="fef-detail-modal-header">
                      <div className="fef-badge-row">
                        <span className="fef-badge fef-badge-pending" style={{ fontSize: "11px" }}>
                          {editingLO.loadingOrderNumber}
                        </span>
                        <span className={`fef-badge fef-badge-${editingLO.status?.toLowerCase()}`}>
                          {editingLO.status}
                        </span>
                      </div>
                      <h2 className="fef-detail-modal-title">Edit Loading Order</h2>
                    </div>

                    <form onSubmit={handleSaveEditLO}>
                      <div className="fef-detail-modal-body" style={{ maxHeight: "65vh", overflowY: "auto", marginTop: 15 }}>
                        <div className="fef-form-grid" style={{ marginBottom: 15 }}>
                          <div className="fef-field">
                            <label className="fef-label">Loading Terminal</label>
                            <input
                              type="text"
                              required
                              className="fef-input"
                              value={editLOForm.loadingTerminal}
                              onChange={(e) => setEditLOForm({ ...editLOForm, loadingTerminal: e.target.value })}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Loading Date</label>
                            <input
                              type="date"
                              required
                              className="fef-input"
                              value={editLOForm.loadingDate}
                              onChange={(e) => setEditLOForm({ ...editLOForm, loadingDate: e.target.value })}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Consignee</label>
                            <input
                              readOnly
                              type="text"
                              className="fef-input"
                              style={{ backgroundColor: "#f3f4f6", cursor: "not-allowed" }}
                              value={editLOForm.consignee}
                            />
                          </div>
                        </div>

                        <div className="fef-form-grid" style={{ marginBottom: 15 }}>
                          <div className="fef-field">
                            <label className="fef-label">Vessel Name</label>
                            <input
                              type="text"
                              required
                              className="fef-input"
                              value={editLOForm.vesselName || ""}
                              onChange={(e) => setEditLOForm({ ...editLOForm, vesselName: e.target.value })}
                            />
                          </div>
                          <div className="fef-field">
                            <label className="fef-label">Operations Manager / Address Info</label>
                            <textarea
                              required
                              className="fef-input"
                              rows="2"
                              value={editLOForm.operationsManager || ""}
                              onChange={(e) => setEditLOForm({ ...editLOForm, operationsManager: e.target.value })}
                            />
                          </div>
                        </div>

                        

                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                          <h5 style={{ margin: 0 }}>Integrated Trucks / Loading Activities</h5>
                          <button
                            type="button"
                            className="fef-btn fef-btn-outline"
                            onClick={handleEditLOAddTruck}
                            style={{ padding: "4px 10px", fontSize: 12 }}
                          >
                            + Add Truck
                          </button>
                        </div>

                        <div style={{ display: "flex", flexDirection: "column", gap: 15, marginBottom: 20 }}>
                          {editLOForm.activities.map((act, idx) => (
                            <div key={idx} style={{ padding: 15, background: "rgba(0,0,0,0.2)", borderRadius: 6, border: "1px solid rgba(255,255,255,0.05)" }}>
                              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                                <strong style={{ fontSize: 13, color: "var(--feftms-primary-light)" }}>Truck #{idx + 1}</strong>
                                <button
                                  type="button"
                                  onClick={() => handleEditLORemoveTruck(idx)}
                                  style={{ background: "none", border: "none", color: "#EF4444", cursor: "pointer", fontSize: 12 }}
                                >
                                  Remove Truck
                                </button>
                              </div>

                              <div className="fef-form-grid" style={{ gap: 12, marginBottom: 12 }}>
                                <div className="fef-field">
                                  <label className="fef-label">Select Registered Vehicle (Optional)</label>
                                  <select
                                    className="fef-input"
                                    value=""
                                    onChange={(e) => {
                                      const vehId = e.target.value;
                                      if (!vehId) return;
                                      const veh = vehiclesList.find(v => v.id.toString() === vehId);
                                      if (veh) {
                                        handleEditLOTruckFieldChange(idx, "truckNumber", veh.plateNumber);
                                        if (veh.driver) {
                                          handleEditLOTruckFieldChange(idx, "driverName", `${veh.driver.firstName} ${veh.driver.lastName}`);
                                          handleEditLOTruckFieldChange(idx, "driverLicenceNumber", veh.driver.licenseNumber);
                                        }
                                      }
                                    }}
                                  >
                                    <option value="">-- Choose Registered Vehicle --</option>
                                    {vehiclesList.map(v => (
                                      <option key={v.id} value={v.id}>{v.plateNumber}</option>
                                    ))}
                                  </select>
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Select Registered Driver (Optional)</label>
                                  <select
                                    className="fef-input"
                                    value=""
                                    onChange={(e) => {
                                      const drvId = e.target.value;
                                      if (!drvId) return;
                                      const drv = driversList.find(d => d.id.toString() === drvId);
                                      if (drv) {
                                        handleEditLOTruckFieldChange(idx, "driverName", `${drv.firstName} ${drv.lastName}`);
                                        handleEditLOTruckFieldChange(idx, "driverLicenceNumber", drv.licenseNumber);
                                      }
                                    }}
                                  >
                                    <option value="">-- Choose Registered Driver --</option>
                                    {driversList.map(d => (
                                      <option key={d.id} value={d.id}>{d.firstName} {d.lastName}</option>
                                    ))}
                                  </select>
                                </div>
                              </div>

                              <div className="fef-form-grid" style={{ gap: 12 }}>
                                <div className="fef-field">
                                  <label className="fef-label">Truck Plate Number</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.truckNumber}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "truckNumber", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Trailer Plate Number</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.trailerNumber}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "trailerNumber", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Driver Name</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.driverName}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "driverName", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Driver Licence Number</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.driverLicenceNumber}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "driverLicenceNumber", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Driver Passport/ID Number</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.driverPassport || ""}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "driverPassport", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Transport Company</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.transportCompany}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "transportCompany", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Destination</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={act.destination}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "destination", e.target.value)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Allocated Quantity (L)</label>
                                  <input
                                    type="number"
                                    required
                                    className="fef-input"
                                    value={act.allocatedQuantity}
                                    onChange={(e) => handleEditLOTruckFieldChange(idx, "allocatedQuantity", e.target.value ? parseFloat(e.target.value) : 0)}
                                  />
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>

                      <div className="fef-detail-modal-footer" style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 20 }}>
                        <button
                          type="button"
                          className="fef-btn fef-btn-outline"
                          onClick={() => setShowEditLOForm(false)}
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          className="fef-btn fef-btn-primary"
                        >
                          Save Changes
                        </button>
                      </div>
                    </form>
                  </div>
                </div>,
                document.body
              )
            }

            {/* Awaiting Loading Orders Section */}
            {true && (
              <div style={{ marginBottom: 35 }}>
                <div className="fef-panel-head" style={{ marginBottom: 15 }}>
                  <h3>Customer Orders Ready for Loading</h3>
                  <span className="fef-badge fef-badge-info" style={{ fontSize: 12 }}>
                    Pending Action: {ordersList.filter(o => o.orderStatus === "READY_FOR_LOADING" && o.paymentStatus === "PAID" && !loadingOrdersList.some(lo => lo.orderId === o.id)).length}
                  </span>
                </div>
                <div className="fef-table-wrap">
                  <table className="fef-table">
                    <thead>
                      <tr>
                        <th>Order #</th>
                        <th>Customer</th>
                        <th>Product</th>
                        <th>Qty (L)</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {ordersList
                        .filter((o) => o.orderStatus === "READY_FOR_LOADING" && o.paymentStatus === "PAID" && !loadingOrdersList.some(lo => lo.orderId === o.id))
                        .map((o) => (
                          <tr key={o.id}>
                            <td>{o.orderNumber}</td>
                            <td>{o.customerName}</td>
                            <td>{o.productName}</td>
                            <td>{(o.approvedQuantity || o.quantity)?.toLocaleString()} L</td>
                            <td>
                              <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                                <span className="fef-badge fef-badge-success" style={{ alignSelf: "flex-start" }}>Ready for Loading</span>
                                <span className={`fef-badge fef-badge-${o.truckNominationStatus === "APPROVED" ? "success" : "warning"}`} style={{ alignSelf: "flex-start", fontSize: 10 }}>
                                  Nomination: {o.truckNominationStatus || "PENDING"}
                                </span>
                              </div>
                            </td>
                            <td>
                              <div style={{ display: "flex", gap: 8 }}>
                                {o.truckNominationStatus === "APPROVED" ? (
                                  <button
                                    className="fef-btn fef-btn-primary"
                                    onClick={() => handleOpenCreateLO(o)}
                                  >
                                    Create Loading Order
                                  </button>
                                ) : (
                                  <button
                                    className="fef-btn fef-btn-primary"
                                    onClick={() => handleOpenReviewNomination(o)}
                                    style={{ background: "#3B82F6" }}
                                  >
                                    Review Nomination
                                  </button>
                                )}
                                <button
                                  className="fef-btn fef-btn-outline"
                                  onClick={() => handleOpenRejectNomination(o)}
                                  style={{ color: "#EF4444", borderColor: "rgba(239, 68, 68, 0.4)" }}
                                >
                                  Request Changes
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      {ordersList.filter((o) => o.orderStatus === "READY_FOR_LOADING" && o.paymentStatus === "PAID" && !loadingOrdersList.some(lo => lo.orderId === o.id)).length === 0 && (
                        <tr>
                          <td colSpan="6" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 20 }}>
                            No pending customer orders ready for loading order creation.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Loading Orders Directory */}
            {true && (
              <div>
                <div className="fef-panel-head" style={{ marginBottom: 15 }}>
                  <h3>Loading Orders Directory</h3>
                </div>
                <div className="fef-table-wrap">
                  <table className="fef-table">
                    <thead>
                      <tr>
                        <th>LO Number</th>
                        <th>Order Ref</th>
                        <th>Customer</th>
                        <th>Date</th>
                        <th>Terminal</th>
                        <th>Trucks</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {loadingOrdersList.map((lo) => (
                        <tr key={lo.id}>
                          <td><strong>{lo.loadingOrderNumber}</strong></td>
                          <td>{lo.customerOrderNumber}</td>
                          <td>{lo.customerName}</td>
                          <td>{lo.loadingDate}</td>
                          <td>{lo.loadingTerminal}</td>
                          <td>{lo.numberOfTrucks}</td>
                          <td>
                            <span className={`fef-badge fef-badge-${lo.status?.toLowerCase()}`}>
                              {lo.status?.replace(/_/g, " ")}
                            </span>
                          </td>
                          <td>
                            <div style={{ display: "flex", gap: 6 }}>
                              <button
                                className="fef-btn fef-btn-primary"
                                onClick={() => setSelectedLoadingOrder(lo)}
                                style={{ padding: "4px 8px", fontSize: 12 }}
                              >
                                View / Load
                              </button>
                              {lo.status === "DRAFT" && (
                                <button
                                  className="fef-btn fef-btn-outline"
                                  onClick={() => handleApproveLO(lo.id)}
                                  style={{ padding: "4px 8px", fontSize: 12, color: "#10B981", borderColor: "rgba(16, 185, 129, 0.4)" }}
                                >
                                  Approve
                                </button>
                              )}
                              {lo.status !== "COMPLETED" && lo.status !== "CANCELLED" && (
                                <button
                                  className="fef-btn fef-btn-outline"
                                  onClick={() => handleCancelLO(lo.id)}
                                  style={{ padding: "4px 8px", fontSize: 12, color: "#EF4444", borderColor: "rgba(239, 68, 68, 0.4)" }}
                                >
                                  Cancel
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                      {loadingOrdersList.length === 0 && (
                        <tr>
                          <td colSpan="8" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 20 }}>
                            No loading orders found in database.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Detailed Loading Order View & Activities Panel */}
            {selectedLoadingOrder &&
              typeof window !== "undefined" &&
              createPortal(
                <div className="fef-modal-backdrop" onClick={() => {
                  setSelectedLoadingOrder(null);
                  loadData();
                }}>
                  <div className="fef-modal-window" style={{ maxWidth: "950px" }} onClick={(e) => e.stopPropagation()}>
                    <button
                      className="fef-modal-close"
                      onClick={() => {
                        setSelectedLoadingOrder(null);
                        loadData();
                      }}
                    >
                      <FiX />
                    </button>
                    
                    <div className="fef-detail-modal-header">
                      <div className="fef-badge-row">
                        <span className="fef-badge fef-badge-pending" style={{ fontSize: "11px" }}>
                          {selectedLoadingOrder.loadingOrderNumber}
                        </span>
                        <span className={`fef-badge fef-badge-${selectedLoadingOrder.status?.toLowerCase()}`}>
                          {selectedLoadingOrder.status}
                        </span>
                      </div>
                      <h2 className="fef-detail-modal-title">Loading Order Details</h2>
                    </div>

                    <div className="fef-detail-modal-body" style={{ maxHeight: "70vh", overflowY: "auto", marginTop: 15 }}>
                      {/* Falcon Loading Order Template Layout */}
                      <div
                        id="printable-loading-order"
                        style={{
                          background: "#ffffff",
                          color: "#000000",
                          border: "3px double #000000",
                          borderRadius: "8px",
                          padding: "40px",
                          marginBottom: 30,
                          fontFamily: "Arial, sans-serif",
                          boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)"
                        }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "2px solid #000000", paddingBottom: 15, marginBottom: 20 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                            <img
                              src={falconLogo}
                              alt="Falcon Energy Logo"
                              style={{ height: 60, width: "auto", objectFit: "contain" }}
                            />
                            <div>
                              <h2 style={{ margin: 0, fontSize: 24, fontWeight: "bold", letterSpacing: "1px" }}>FALCON ENERGY LIMITED</h2>
                              <p style={{ margin: "4px 0 0 0", fontSize: 12 }}>Fuel Distribution & Distribution Logistics</p>
                            </div>
                          </div>
                          <div style={{ textAlign: "right" }}>
                            <h3 style={{ margin: 0, fontSize: 18, fontWeight: "bold", color: "#EF4444" }}>LOADING ORDER</h3>
                            <span className={`fef-badge fef-badge-${selectedLoadingOrder.status?.toLowerCase()}`} style={{ fontSize: 11 }}>
                              {selectedLoadingOrder.status}
                            </span>
                          </div>
                        </div>

                        {/* Top Metadata Row */}
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 25, fontSize: 13 }}>
                          <div>
                            <span style={{ fontWeight: "bold", fontSize: 11, letterSpacing: "0.5px", color: "#555" }}>OPERATIONS MANAGER</span>
                            <div style={{ fontWeight: "bold", marginTop: 4, whiteSpace: "pre-line" }}>
                              {selectedLoadingOrder.operationsManager || "GSG ENERGIES\nDAR ES SALAAM"}
                            </div>
                          </div>
                          <div style={{ textAlign: "right" }}>
                            <div style={{ display: "inline-flex", alignItems: "center", gap: 10 }}>
                              <span style={{ fontWeight: "bold" }}>DATE:</span>
                              <span style={{ background: "#86EFAC", padding: "4px 12px", borderRadius: 4, fontWeight: "bold" }}>
                                {selectedLoadingOrder.loadingDate}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Info Highlight Box */}
                        <div style={{ border: "1px solid #cbd5e1", borderRadius: 4, background: "#f8fafc", padding: "15px 20px", marginBottom: 25, display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px 40px", fontSize: 12 }}>
                          <div>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "140px" }}>LOADING TERMINAL:</span>
                            <span>{selectedLoadingOrder.loadingTerminal}</span>
                          </div>
                          <div>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "140px" }}>VESSEL NAME:</span>
                            <span style={{ background: "#FEF08A", padding: "2px 6px", borderRadius: 3, fontWeight: "bold" }}>
                              {selectedLoadingOrder.vesselName || "TBA"}
                            </span>
                          </div>
                          <div>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "140px" }}>PRODUCT NAME:</span>
                            <span style={{ background: "#FEF08A", padding: "2px 6px", borderRadius: 3, fontWeight: "bold" }}>
                              {selectedLoadingOrder.product}
                            </span>
                          </div>
                          <div>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "140px" }}>NUMBER OF TRUCKS:</span>
                            <span style={{ background: "#FEF08A", padding: "2px 6px", borderRadius: 3, fontWeight: "bold" }}>
                              {selectedLoadingOrder.numberOfTrucks || selectedLoadingOrder.activities?.length || 0}
                            </span>
                          </div>
                          <div style={{ gridColumn: "span 2" }}>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "140px" }}>CONSIGNEE NAME:</span>
                            <span style={{ fontWeight: "bold" }}>{selectedLoadingOrder.consignee}</span>
                          </div>
                        </div>

                        {/* Main Table Title Badge */}
                        <div style={{ background: "#15803D", color: "#ffffff", padding: "6px 12px", textAlign: "center", fontWeight: "bold", fontSize: 12, textTransform: "uppercase", letterSpacing: "1px", marginBottom: 10 }}>
                          FALCON ENERGY - LOADING ORDER
                        </div>

                        {/* Printable Table */}
                        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 11, border: "1px solid #000" }}>
                          <thead>
                            <tr style={{ background: "#86EFAC", borderBottom: "2px solid #000" }}>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "center" }}>S. NO</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>DATE</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>ORDER NO</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>PRODUCT</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "right" }}>QUANTITY (LTRS)</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>DRIVER NAME</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>DRIVER LICENCE</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>PASSPORT NO</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>TRUCK NO</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>TRAILER NO</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>TRANSPORTER</th>
                              <th style={{ border: "1px solid #000", padding: 6, textAlign: "left" }}>DESTINATION</th>
                            </tr>
                          </thead>
                          <tbody>
                            {selectedLoadingOrder.activities?.map((act, idx) => (
                              <tr key={idx} style={{ borderBottom: "1px solid #000" }}>
                                <td style={{ border: "1px solid #000", padding: 6, textAlign: "center" }}>{idx + 1}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{selectedLoadingOrder.loadingDate}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{selectedLoadingOrder.customerOrderNumber}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{selectedLoadingOrder.product}</td>
                                <td style={{ border: "1px solid #000", padding: 6, textAlign: "right", fontWeight: "bold" }}>{act.allocatedQuantity?.toLocaleString()}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.driverName}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.driverLicenceNumber}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.driverPassport || act.passportNumber || "N/A"}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.truckNumber}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.trailerNumber || "N/A"}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.transportCompany}</td>
                                <td style={{ border: "1px solid #000", padding: 6 }}>{act.destination}</td>
                              </tr>
                            ))}
                            <tr style={{ background: "#f8fafc", fontWeight: "bold" }}>
                              <td colSpan="4" style={{ border: "1px solid #000", padding: 6, textAlign: "right" }}>TOTAL QTY:</td>
                              <td style={{ border: "1px solid #000", padding: 6, textAlign: "right" }}>
                                {selectedLoadingOrder.activities?.reduce((sum, act) => sum + (act.allocatedQuantity || 0), 0).toLocaleString()}
                              </td>
                              <td colSpan="7" style={{ border: "1px solid #000" }}></td>
                            </tr>
                          </tbody>
                        </table>

                        {/* Signatures Row */}
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 40, fontSize: 12 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "100px" }}>Prepared By:</span>
                            <span style={{ borderBottom: "1px dotted #000", minWidth: 180, textAlign: "left", paddingBottom: 4, fontWeight: "bold" }}>
                              {selectedLoadingOrder.preparedBy || "Palibe Kaude"}
                            </span>
                          </div>
                          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                            <span style={{ fontWeight: "bold" }}>Signature:</span>
                            <span style={{ borderBottom: "1px dotted #000", minWidth: 200, display: "inline-block", height: 16 }}></span>
                          </div>
                        </div>

                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 25, fontSize: 12 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                            <span style={{ fontWeight: "bold", display: "inline-block", width: "100px" }}>Authorized By:</span>
                            <span style={{ borderBottom: "1px dotted #000", minWidth: 180, textAlign: "left", paddingBottom: 4, fontWeight: "bold" }}>
                              {selectedLoadingOrder.approvedBy || "Not Authorized Yet"}
                            </span>
                          </div>
                          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                            <span style={{ fontWeight: "bold" }}>Signature:</span>
                            <span style={{ borderBottom: "1px dotted #000", minWidth: 200, display: "inline-block", height: 16 }}></span>
                          </div>
                        </div>
                      </div>

                      {/* Loading Activities Management Board */}
                      <div className="fef-card" style={{ padding: 20 }}>
                        <h3 style={{ marginTop: 0, marginBottom: 15, display: "flex", alignItems: "center", gap: 8 }}>
                          <FiActivity /> Live Truck Loading Activities
                        </h3>

                        {!["APPROVED", "LOADING_IN_PROGRESS", "COMPLETED"].includes(selectedLoadingOrder.status) ? (
                          <div className="fef-alert fef-alert-info">
                            <FiInfo style={{ verticalAlign: "-2px", marginRight: 6 }} />
                            Loading activities can only start after the Loading Order is approved and locked.
                          </div>
                        ) : (
                          <div style={{ display: "flex", flexDirection: "column", gap: 15 }}>
                            {selectedLoadingOrder.activities?.map((act) => (
                              <div
                                key={act.id}
                                style={{
                                  padding: 15,
                                  background: "rgba(0,0,0,0.2)",
                                  borderRadius: 6,
                                  border: "1px solid rgba(255,255,255,0.05)",
                                  display: "flex",
                                  justifyContent: "space-between",
                                  alignItems: "center"
                                }}
                              >
                                <div>
                                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                                    <strong style={{ fontSize: 15, color: "var(--feftms-primary-light)" }}>{act.truckNumber}</strong>
                                    <span className={`fef-badge fef-badge-${act.status?.toLowerCase()}`} style={{ fontSize: 10, padding: "2px 6px" }}>
                                      {act.status}
                                    </span>
                                  </div>
                                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "10px 30px", marginTop: 8, fontSize: 12, color: "var(--feftms-text-muted)" }}>
                                    <div>Driver: <strong>{act.driverName}</strong></div>
                                    <div>Company: <strong>{act.transportCompany}</strong></div>
                                    <div>Allocated: <strong>{act.allocatedQuantity?.toLocaleString()} L</strong></div>
                                    <div>Queue No: <strong>{act.queueNumber}</strong></div>
                                    <div>Bay No: <strong>{act.bayNumber}</strong></div>
                                    <div>Pump No: <strong>{act.pumpNumber || "N/A"}</strong></div>
                                  </div>

                                  {act.loadingStartTime && (
                                    <div style={{ marginTop: 8, fontSize: 11, color: "var(--feftms-text-muted)" }}>
                                      ⏱️ Started: {new Date(act.loadingStartTime).toLocaleString()} by {act.loadingOfficer}
                                      {act.loadingCompletionTime && ` | Completed: ${new Date(act.loadingCompletionTime).toLocaleString()}`}
                                    </div>
                                  )}
                                </div>

                                <div>
                                  {act.status === "WAITING" && (
                                    <div style={{ display: "flex", flexDirection: "column", gap: 8, alignItems: "flex-end" }}>
                                      <div style={{ display: "flex", gap: 8 }}>
                                        <input
                                          type="text"
                                          placeholder="Bay (default: BAY-1)"
                                          className="fef-input"
                                          style={{ width: 140, padding: "4px 8px", fontSize: 12, height: 32, color: "#fff", background: "rgba(0,0,0,0.3)" }}
                                          value={activityInputs[act.id]?.bayNumber || ""}
                                          onChange={(e) => handleActivityInputChange(act.id, "bayNumber", e.target.value)}
                                        />
                                        <input
                                          type="text"
                                          placeholder="Pump (optional)"
                                          className="fef-input"
                                          style={{ width: 120, padding: "4px 8px", fontSize: 12, height: 32, color: "#fff", background: "rgba(0,0,0,0.3)" }}
                                          value={activityInputs[act.id]?.pumpNumber || ""}
                                          onChange={(e) => handleActivityInputChange(act.id, "pumpNumber", e.target.value)}
                                        />
                                      </div>
                                      <button
                                        className="fef-btn fef-btn-primary"
                                        onClick={() => handleStartLoading(selectedLoadingOrder.id, act.id)}
                                        style={{ background: "#3B82F6", height: 32, padding: "4px 12px", fontSize: 12 }}
                                      >
                                        Start Loading
                                      </button>
                                    </div>
                                  )}
                                  {act.status === "LOADING" && (
                                    <button
                                      className="fef-btn fef-btn-primary"
                                      onClick={() => handleCompleteLoading(selectedLoadingOrder.id, act.id)}
                                      style={{ background: "#10B981" }}
                                    >
                                      Complete Loading
                                    </button>
                                  )}
                                  {act.status === "LOADED" && (
                                    <div style={{ display: "flex", alignItems: "center", gap: 4, color: "#10B981", fontSize: 13, fontWeight: "bold" }}>
                                      <FiCheckCircle /> Loaded
                                    </div>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="fef-detail-modal-footer" style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 20 }}>
                      <button
                        className="fef-btn fef-btn-outline"
                        onClick={handlePrintLO}
                        style={{ display: "flex", alignItems: "center", gap: 6 }}
                      >
                        <FiPrinter size={16} /> Print Loading Order
                      </button>
                      {selectedLoadingOrder.status === "DRAFT" && (
                        <>
                          <button
                            className="fef-btn fef-btn-primary"
                            onClick={() => handleApproveLO(selectedLoadingOrder.id)}
                            style={{ background: "#10B981" }}
                          >
                            Approve & Lock Loading Order
                          </button>
                          <button
                            className="fef-btn fef-btn-primary"
                            onClick={() => handleOpenEditLO(selectedLoadingOrder)}
                            style={{ background: "#3B82F6" }}
                          >
                            Edit Loading Order
                          </button>
                        </>
                      )}
                      {selectedLoadingOrder.status !== "COMPLETED" && selectedLoadingOrder.status !== "CANCELLED" && (
                        <button
                          className="fef-btn fef-btn-outline"
                          onClick={() => handleCancelLO(selectedLoadingOrder.id)}
                          style={{ color: "#EF4444", borderColor: "rgba(239, 68, 68, 0.4)" }}
                        >
                          Cancel Loading Order
                        </button>
                      )}
                      <button
                        className="fef-btn fef-btn-outline"
                        onClick={() => {
                          setSelectedLoadingOrder(null);
                          loadData();
                        }}
                      >
                        Close
                      </button>
                    </div>
                  </div>
                </div>,
                document.body
              )
            }
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
                            className={`fef-badge fef-badge-${d.order.emergencyLevel.toLowerCase().includes("critical") ? "danger" : "info"}`}
                            style={{
                              fontSize: 10,
                              padding: "2px 6px",
                              marginTop: 4,
                              display: "inline-block",
                            }}
                          >
                            {d.order.emergencyLevel}
                          </span>
                        )}
                        {d.order?.driverName && (
                          <div
                            style={{
                              fontSize: 11,
                              color: "var(--feftms-text-muted)",
                              marginTop: 4,
                            }}
                          >
                            Customer: {d.order.driverName} ({d.order.driverPhone})
                          </div>
                        )}
                        {d.order?.locationAddress && (
                          <div
                            style={{
                              fontSize: 10,
                              color: "var(--feftms-text-muted)",
                              marginTop: 2,
                            }}
                            title={`GPS: ${d.order.locationGps || "N/A"} | Landmark: ${d.order.locationLandmark || "N/A"}`}
                          >
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
              <div
                className="fef-card"
                style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}
              >
                <h4>Register Operational Driver</h4>
                <form onSubmit={handleCreateDriver} style={{ marginTop: 15 }}>
                  <div className="fef-form-grid">
                    <div className="fef-field">
                      <label className="fef-label">First Name</label>
                      <input
                        required
                        className="fef-input"
                        value={driverForm.firstName}
                        onChange={(e) =>
                          setDriverForm({ ...driverForm, firstName: e.target.value })
                        }
                        placeholder="Peter"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Last Name</label>
                      <input
                        required
                        className="fef-input"
                        value={driverForm.lastName}
                        onChange={(e) => setDriverForm({ ...driverForm, lastName: e.target.value })}
                        placeholder="Maina"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Phone Number</label>
                      <input
                        className="fef-input"
                        value={driverForm.phone}
                        onChange={(e) => setDriverForm({ ...driverForm, phone: e.target.value })}
                        placeholder="+254 711 000000"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Driving License Number</label>
                      <input
                        required
                        className="fef-input"
                        value={driverForm.licenseNumber}
                        onChange={(e) =>
                          setDriverForm({ ...driverForm, licenseNumber: e.target.value })
                        }
                        placeholder="DL-123456"
                      />
                    </div>
                  </div>
                  <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                    <button type="submit" className="fef-btn fef-btn-primary">
                      Register
                    </button>
                    <button
                      type="button"
                      className="fef-btn fef-btn-outline"
                      onClick={() => setShowDriverModal(false)}
                    >
                      Cancel
                    </button>
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
                      <td>
                        {d.firstName} {d.lastName}
                      </td>
                      <td>{d.phone || "—"}</td>
                      <td>{d.licenseNumber}</td>
                      <td>
                        <span
                          className={`fef-badge fef-badge-${d.status === "AVAILABLE" ? "approved" : "progress"}`}
                        >
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
              <div
                className="fef-card"
                style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}
              >
                <h4>Register Fleet Vehicle</h4>
                <form onSubmit={handleCreateVehicle} style={{ marginTop: 15 }}>
                  <div className="fef-form-grid">
                    <div className="fef-field">
                      <label className="fef-label">Plate Number</label>
                      <input
                        required
                        className="fef-input"
                        value={vehicleForm.plateNumber}
                        onChange={(e) =>
                          setVehicleForm({ ...vehicleForm, plateNumber: e.target.value })
                        }
                        placeholder="KDA 432X"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Max Capacity (Litres)</label>
                      <input
                        required
                        type="number"
                        className="fef-input"
                        value={vehicleForm.capacity}
                        onChange={(e) =>
                          setVehicleForm({ ...vehicleForm, capacity: e.target.value })
                        }
                        placeholder="35000"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Link Driver (Optional)</label>
                      <select
                        className="fef-select"
                        value={vehicleForm.driverId}
                        onChange={(e) =>
                          setVehicleForm({ ...vehicleForm, driverId: e.target.value })
                        }
                      >
                        <option value="">-- No Assigned Driver --</option>
                        {driversList.map((d) => (
                          <option key={d.id} value={d.id}>
                            {d.firstName} {d.lastName}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                    <button type="submit" className="fef-btn fef-btn-primary">
                      Register
                    </button>
                    <button
                      type="button"
                      className="fef-btn fef-btn-outline"
                      onClick={() => setShowVehicleModal(false)}
                    >
                      Cancel
                    </button>
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
                      <td>
                        <strong>{v.plateNumber}</strong>
                      </td>
                      <td>{v.capacity?.toLocaleString()} L</td>
                      <td>{v.driverName || "—"}</td>
                      <td>
                        <span className="fef-badge fef-badge-approved">{v.currentStatus}</span>
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
              <button
                className="fef-btn fef-btn-primary"
                onClick={() => setShowCreateProductModal(true)}
              >
                <FiPlus /> Add New Fuel Product
              </button>
            </div>

            {showCreateProductModal && (
              <div className="fef-modal-overlay" onClick={() => setShowCreateProductModal(false)}>
                <div className="fef-modal" onClick={(e) => e.stopPropagation()}>
                  <h2 className="fef-detail-modal-title">Create New Fuel Product</h2>
                  <form
                    onSubmit={handleCreateProduct}
                    style={{ display: "flex", flexDirection: "column", gap: 12 }}
                  >
                    <div className="fef-field">
                      <label className="fef-label">Product Name</label>
                      <input
                        required
                        type="text"
                        className="fef-input"
                        value={createProductForm.productName}
                        onChange={(e) =>
                          setCreateProductForm({
                            ...createProductForm,
                            productName: e.target.value,
                          })
                        }
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
                        onChange={(e) =>
                          setCreateProductForm({ ...createProductForm, fuelType: e.target.value })
                        }
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
                        onChange={(e) =>
                          setCreateProductForm({ ...createProductForm, density: e.target.value })
                        }
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
                        onChange={(e) =>
                          setCreateProductForm({
                            ...createProductForm,
                            availableQuantity: e.target.value,
                          })
                        }
                        placeholder="0"
                      />
                    </div>
                    <p style={{ fontSize: 12, color: "var(--feftms-text-muted)", marginBottom: 0 }}>
                      Note: Finance will set the unit price after this product is created.
                    </p>
                    <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                      <button type="submit" className="fef-btn fef-btn-primary">
                        Create Product
                      </button>
                      <button
                        type="button"
                        className="fef-btn fef-btn-outline"
                        onClick={() => setShowCreateProductModal(false)}
                      >
                        Cancel
                      </button>
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
                          <td style={{ fontWeight: 700 }}>
                            {t.currentVolume?.toLocaleString() || 0} L
                          </td>
                          <td>
                            <span
                              className={`fef-badge fef-badge-${(t.status || "").toLowerCase() === "active" ? "success" : "danger"}`}
                            >
                              {t.status}
                            </span>
                          </td>
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
                      <td
                        colSpan="7"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No fuel products yet. Click "Add New Fuel Product" to create one.
                      </td>
                    </tr>
                  ) : (
                    productsList.map((p) => (
                      <tr key={p.id}>
                        <td>
                          <strong>{p.productName}</strong>
                        </td>
                        <td>{p.fuelType}</td>
                        <td style={{ color: "var(--feftms-text-muted)" }}>
                          ${p.unitPrice?.toFixed(2)}/L
                        </td>
                        <td>{p.density} kg/L</td>
                        <td
                          style={{
                            fontWeight: 700,
                            color:
                              p.availableQuantity > 0
                                ? "var(--feftms-success)"
                                : "var(--feftms-danger)",
                          }}
                        >
                          {p.availableQuantity?.toLocaleString()} L
                        </td>
                        <td>
                          <span
                            className={`fef-badge fef-badge-${p.status?.toLowerCase() === "active" || p.status?.toLowerCase() === "available" ? "success" : "danger"}`}
                          >
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
        {showStockModal &&
          selectedProduct &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setShowStockModal(false)}>
              <div
                className="fef-modal-window"
                style={{ maxWidth: "500px" }}
                onClick={(e) => e.stopPropagation()}
              >
                <button className="fef-modal-close" onClick={() => setShowStockModal(false)}>
                  <FiX />
                </button>
                <div className="fef-detail-modal-header">
                  <h2 className="fef-detail-modal-title">Inventory Adjustment</h2>
                  <p style={{ margin: "4px 0 0", color: "var(--feftms-text-muted)" }}>
                    Update available stock for {selectedProduct.productName} (Current:{" "}
                    {selectedProduct.availableQuantity?.toLocaleString()} L)
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
                        <option value="SUBTRACT">
                          Inventory Adjustment / Shrinkage (Decrease Stock)
                        </option>
                        <option value="STATUS_ONLY">
                          Manual Availability Status Override Only
                        </option>
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
                        onChange={(e) =>
                          setStockForm({ ...stockForm, overrideStatus: e.target.value })
                        }
                      >
                        <option value="">-- Auto Determine (Active if stock &gt; 0) --</option>
                        <option value="ACTIVE">FORCE AVAILABLE (ACTIVE)</option>
                        <option value="UNAVAILABLE">
                          FORCE UNAVAILABLE (Tank maintenance, contamination, etc.)
                        </option>
                      </select>
                    </div>

                    <div className="fef-field" style={{ opacity: 0.6 }}>
                      <label className="fef-label">
                        Selling Price per Litre (Locked — Finance Only)
                      </label>
                      <input
                        disabled
                        type="text"
                        className="fef-input"
                        value={`$${selectedProduct.unitPrice?.toFixed(2)}/L (Cannot modify)`}
                      />
                    </div>
                  </div>

                  <div className="fef-detail-actions" style={{ marginTop: 24 }}>
                    <button
                      type="button"
                      className="fef-btn fef-btn-outline"
                      onClick={() => setShowStockModal(false)}
                    >
                      Cancel
                    </button>
                    <button type="submit" className="fef-btn fef-btn-primary">
                      <FiCheck style={{ marginRight: 4 }} /> Save Inventory Updates
                    </button>
                  </div>
                </form>
              </div>
            </div>,
            document.body,
          )}
      </DashboardLayout>
    </RouteGuard>
  );
}
