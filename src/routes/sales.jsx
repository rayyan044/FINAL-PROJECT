import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import {
  FiClock,
  FiCheckSquare,
  FiFileText,
  FiUsers,
  FiHome,
  FiClipboard,
  FiPlus,
  FiCheck,
  FiX,
  FiAlertCircle,
  FiCheckCircle,
  FiMapPin,
  FiPhone,
  FiMail,
  FiInfo,
  FiDollarSign,
  FiEdit,
  FiTruck,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import {
  listRequests,
  updateRequestStatus,
  approveRequestWithEdit,
} from "../services/requestService";
import { listCustomers, createCustomer } from "../services/customerService";
import { listProducts } from "../services/productService";
import { getInvoiceById } from "../services/invoiceService";
import { InvoiceModal } from "../components/InvoiceModal";
import {
  createNomination,
  updateNomination,
  getNominationByOrderId,
  submitNomination,
  cancelNomination,
} from "../services/truckNominationService";
import { listVehicles } from "../services/vehicleService";
import { listDrivers } from "../services/driverService";
import "../styles/forms.css";

export const Route = createFileRoute("/sales")({
  head: () => ({ meta: [{ title: "Sales Officer — FEFTMS" }] }),
  component: SalesDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "requests", label: "Requests & Approvals", icon: FiClipboard },
  { key: "nomination", label: "Truck Nomination", icon: FiTruck },
  { key: "revisions", label: "Revisions Requested", icon: FiAlertCircle },
  { key: "inventory", label: "Fuel Products", icon: FiFileText },
  { key: "customers", label: "Customers Directory", icon: FiUsers },
];

function SalesDash() {
  const [activeTab, setActiveTab] = useState("dash");
  const [stats, setStats] = useState({ pending: 0, approved: 0, customers: 0 });
  const [ordersList, setOrdersList] = useState([]);
  const [customersList, setCustomersList] = useState([]);
  const [productsList, setProductsList] = useState([]);
  const [vehiclesList, setVehiclesList] = useState([]);
  const [driversList, setDriversList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  // Invoice & Editing States
  const [isEditing, setIsEditing] = useState(false);
  const [editQty, setEditQty] = useState("");
  const [editReason, setEditReason] = useState("");
  const [validationError, setValidationError] = useState("");

  // Customer creation form modal
  const [showModal, setShowModal] = useState(false);
  const [custForm, setCustForm] = useState({
    customerCode: "",
    companyName: "",
    contactPerson: "",
    phone: "",
    email: "",
    address: "",
    tinNumber: "",
  });

  // Truck Nomination States
  const [selectedNominationOrder, setSelectedNominationOrder] = useState(null);
  const [nominationForm, setNominationForm] = useState({
    id: null,
    orderId: null,
    transportSource: "CUSTOMER_TRUCKS",
    numberOfTrucks: "",
    confirmationNotes: "",
    rejectionReason: "",
    items: [],
  });
  const [nominationError, setNominationError] = useState("");
  const [nominationSuccess, setNominationSuccess] = useState("");
  const [nominationLoading, setNominationLoading] = useState(false);

  const handleOpenNomination = async (order) => {
    setNominationLoading(true);
    setNominationError("");
    setNominationSuccess("");
    setSelectedNominationOrder(order);

    try {
      const nomination = await getNominationByOrderId(order.id);
      setNominationForm({
        id: nomination.id,
        orderId: order.id,
        transportSource: nomination.transportSource || "CUSTOMER_TRUCKS",
        numberOfTrucks: nomination.numberOfTrucks || "",
        confirmationNotes: nomination.confirmationNotes || "",
        rejectionReason: nomination.rejectionReason || "",
        items: nomination.items || [],
      });
    } catch (err) {
      // Nomination not found, initialize new draft
      setNominationForm({
        id: null,
        orderId: order.id,
        transportSource: "CUSTOMER_TRUCKS",
        numberOfTrucks: "",
        confirmationNotes: "",
        rejectionReason: "",
        items: [],
      });
    } finally {
      setNominationLoading(false);
    }
  };

  const handleSaveNominationDraft = async () => {
    setNominationError("");
    setNominationSuccess("");
    setNominationLoading(true);

    try {
      let res;
      const payload = {
        orderId: nominationForm.orderId,
        transportSource: nominationForm.transportSource,
        numberOfTrucks: nominationForm.numberOfTrucks ? parseInt(nominationForm.numberOfTrucks) : null,
        confirmationNotes: nominationForm.confirmationNotes,
        items: nominationForm.items,
      };

      if (nominationForm.id) {
        res = await updateNomination(nominationForm.id, payload);
        setNominationSuccess("Truck nomination draft updated successfully.");
      } else {
        res = await createNomination(payload);
        const saved = res.data || res;
        setNominationForm(prev => ({ ...prev, id: saved.id }));
        setNominationSuccess("Truck nomination draft created successfully.");
      }
      loadData();
    } catch (err) {
      setNominationError(err?.response?.data?.message || err?.message || "Failed to save nomination draft.");
    } finally {
      setNominationLoading(false);
    }
  };

  const handleSubmitNomination = async () => {
    setNominationError("");
    setNominationSuccess("");

    const orderQty = selectedNominationOrder.approvedQuantity || selectedNominationOrder.quantity || 0;
    const totalAllocated = nominationForm.items.reduce((sum, item) => sum + parseFloat(item.allocatedQuantity || 0), 0);

    if (nominationForm.transportSource === "CUSTOMER_TRUCKS") {
      if (!nominationForm.numberOfTrucks || parseInt(nominationForm.numberOfTrucks, 10) <= 0) {
        setNominationError("Number of Trucks is required and must be greater than 0 for Customer Transport.");
        return;
      }
      if (nominationForm.items.length === 0) {
        setNominationError("At least one truck must be nominated.");
        return;
      }
      for (let i = 0; i < nominationForm.items.length; i++) {
        const item = nominationForm.items[i];
        if (!item.truckNumber?.trim()) {
          setNominationError(`Truck #${i + 1} Plate Number is required.`);
          return;
        }
        if (!item.driverName?.trim()) {
          setNominationError(`Truck #${i + 1} Driver Name is required.`);
          return;
        }
        if (!item.driverLicenceNumber?.trim()) {
          setNominationError(`Truck #${i + 1} Driver Licence Number is required.`);
          return;
        }
        if (!item.allocatedQuantity || parseFloat(item.allocatedQuantity) <= 0) {
          setNominationError(`Truck #${i + 1} Allocated Quantity must be greater than 0 L.`);
          return;
        }
      }
      if (totalAllocated !== orderQty) {
        setNominationError(`Total allocated quantity (${totalAllocated.toLocaleString()} L) must exactly equal approved customer order quantity (${orderQty.toLocaleString()} L) for Customer Transport.`);
        return;
      }
    } else {
      if (totalAllocated > orderQty) {
        setNominationError("Total allocated quantity cannot exceed approved customer order quantity.");
        return;
      }
    }

    setNominationLoading(true);

    try {
      // First save draft changes
      const payload = {
        orderId: nominationForm.orderId,
        transportSource: nominationForm.transportSource,
        numberOfTrucks: nominationForm.numberOfTrucks ? parseInt(nominationForm.numberOfTrucks) : null,
        confirmationNotes: nominationForm.confirmationNotes,
        items: nominationForm.items,
      };

      let nominationId = nominationForm.id;
      if (nominationId) {
        await updateNomination(nominationId, payload);
      } else {
        const createRes = await createNomination(payload);
        const saved = createRes.data || createRes;
        nominationId = saved.id;
      }

      await submitNomination(nominationId);
      setNominationSuccess("Truck nomination submitted successfully. Order is now READY FOR LOADING.");
      setSelectedNominationOrder(null);
      loadData();
    } catch (err) {
      setNominationError(err?.response?.data?.message || err?.message || "Failed to submit nomination.");
    } finally {
      setNominationLoading(false);
    }
  };

  const handleCancelNominationClick = async () => {
    if (!nominationForm.id) return;
    if (!window.confirm("Are you sure you want to cancel this truck nomination? This will reset the order status.")) return;
    setNominationLoading(true);
    setNominationError("");
    setNominationSuccess("");
    try {
      await cancelNomination(nominationForm.id);
      setNominationSuccess("Truck nomination cancelled successfully.");
      setSelectedNominationOrder(null);
      loadData();
    } catch (err) {
      setNominationError(err?.response?.data?.message || err?.message || "Failed to cancel nomination.");
    } finally {
      setNominationLoading(false);
    }
  };

  const handleAddTruckItem = () => {
    const newItem = {
      truckNumber: "",
      trailerNumber: "",
      driverName: "",
      driverLicenceNumber: "",
      driverPassport: "",
      transportCompany: nominationForm.transportSource === "CUSTOMER_TRUCKS" ? (selectedNominationOrder.customerName || "") : "",
      destination: selectedNominationOrder.destination || "",
      truckCapacity: 0,
      allocatedQuantity: 0,
    };

    setNominationForm(prev => ({
      ...prev,
      items: [...prev.items, newItem]
    }));
  };

  const handleRemoveTruckItem = (index) => {
    setNominationForm(prev => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== index)
    }));
  };

  const handleUpdateTruckItem = (index, field, value) => {
    setNominationForm(prev => {
      const newItems = [...prev.items];
      newItems[index] = { ...newItems[index], [field]: value };
      return { ...prev, items: newItems };
    });
  };

  const handleTransportSourceChange = (val) => {
    const isLocked = ["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus);
    if (isLocked) return;

    if (val === "FALCON_ARRANGED" && nominationForm.transportSource === "CUSTOMER_TRUCKS") {
      const hasUnsavedData = nominationForm.numberOfTrucks || nominationForm.items.length > 0;
      if (hasUnsavedData) {
        const confirm = window.confirm("Switching to Falcon Arranged Transport will clear any unsaved customer truck registration data. Proceed?");
        if (!confirm) return;
      }
      setNominationForm(prev => ({
        ...prev,
        transportSource: val,
        numberOfTrucks: "",
        items: []
      }));
    } else {
      setNominationForm(prev => ({
        ...prev,
        transportSource: val
      }));
    }
  };

  const handleNumberOfTrucksChange = (val) => {
    const isLocked = ["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus);
    if (isLocked) return;

    const count = val === "" ? 0 : parseInt(val, 10);
    if (isNaN(count) || count < 0) return;

    const currentItems = nominationForm.items || [];
    const prevCount = currentItems.length;

    if (count > prevCount) {
      const newItems = [...currentItems];
      for (let i = prevCount; i < count; i++) {
        newItems.push({
          truckNumber: "",
          trailerNumber: "",
          driverName: "",
          driverLicenceNumber: "",
          driverPassport: "",
          transportCompany: nominationForm.transportSource === "CUSTOMER_TRUCKS" ? (selectedNominationOrder.customerName || "") : "",
          destination: selectedNominationOrder.destination || "",
          truckCapacity: 0,
          allocatedQuantity: 0,
        });
      }
      setNominationForm(prev => ({
        ...prev,
        numberOfTrucks: val,
        items: newItems
      }));
    } else if (count < prevCount) {
      const confirm = window.confirm("You are reducing the number of trucks. This will remove excess entered truck forms. Do you want to proceed?");
      if (!confirm) return;

      const newItems = currentItems.slice(0, count);
      setNominationForm(prev => ({
        ...prev,
        numberOfTrucks: val,
        items: newItems
      }));
    } else {
      setNominationForm(prev => ({
        ...prev,
        numberOfTrucks: val
      }));
    }
  };

  const loadData = () => {
    setLoading(true);
    setError("");

    Promise.all([
      listRequests({ size: 100 }),
      listCustomers({ size: 100 }),
      listProducts({ size: 100 }),
      listVehicles({ size: 100 }),
      listDrivers({ size: 100 }),
    ])
      .then(([reqs, custs, prods, vehs, drvs]) => {
        const content = reqs.content || [];
        const pendingCount = content.filter((o) => o.orderStatus === "PENDING").length;
        const approvedCount = content.filter((o) => o.orderStatus === "APPROVED").length;

        setStats({
          pending: pendingCount,
          approved: approvedCount,
          customers: custs.totalElements || 0,
        });

        setOrdersList(content);
        setCustomersList(custs.content || []);
        setProductsList(prods.content || []);
        setVehiclesList(vehs.content || []);
        setDriversList(drvs.content || []);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Error connecting to database. Please check the backend connection.");
        setLoading(false);
      });
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(() => loadData(), 15000);
    return () => clearInterval(interval);
  }, [activeTab]);

  const handleApprove = async (id) => {
    setError("");
    setSuccess("");
    try {
      await updateRequestStatus(id, "APPROVED");
      setSuccess("Order approved successfully");
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to approve order.");
    }
  };

  const handleReject = async (id) => {
    setError("");
    setSuccess("");
    try {
      await updateRequestStatus(id, "REJECTED");
      setSuccess("Order marked as REJECTED");
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to update order status.");
    }
  };

  const handleApproveEdited = async (id) => {
    setValidationError("");
    setError("");
    setSuccess("");
    try {
      const parsedQty = parseFloat(editQty);
      if (isNaN(parsedQty) || parsedQty <= 0) {
        setValidationError("Approved quantity must be a positive number.");
        return;
      }

      const req = ordersList.find((o) => o.id === id);
      if (!req) return;

      const prod = productsList.find(
        (p) => p.id === req.product?.id || p.productName === req.productName,
      );
      const availableStock = prod ? prod.availableQuantity : 0;

      if (parsedQty > availableStock) {
        setValidationError(
          `Approved quantity (${parsedQty.toLocaleString()} L) still exceeds current available stock of ${availableStock.toLocaleString()} L.`,
        );
        return;
      }

      await approveRequestWithEdit(id, parsedQty, editReason);
      setSuccess("Order approved with updated quantity successfully");
      setSelectedRequest(null);
      setIsEditing(false);
      setEditQty("");
      setEditReason("");
      loadData();
    } catch (err) {
      setValidationError(
        err?.response?.data?.message || err?.message || "Failed to approve edited order.",
      );
    }
  };

  const handleViewInvoice = async (invoiceId) => {
    setLoading(true);
    setError("");
    try {
      const res = await getInvoiceById(invoiceId);
      setSelectedInvoice(res);
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to load invoice details.");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCustomer = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const code = custForm.customerCode || "CUST-" + Math.floor(1000 + Math.random() * 9000);
      await createCustomer({
        ...custForm,
        customerCode: code,
        status: "ACTIVE",
      });
      setSuccess("Customer registered successfully");
      setShowModal(false);
      setCustForm({
        customerCode: "",
        companyName: "",
        contactPerson: "",
        phone: "",
        email: "",
        address: "",
        tinNumber: "",
      });
      loadData();
    } catch (err) {
      setError(err?.message || "Failed to create customer.");
    }
  };

  return (
    <RouteGuard allowedRoles={["SALES_OFFICER"]}>
      <DashboardLayout
        role="Sales Officer"
        userName="Sarah Kamau"
        pageTitle="Sales Desk"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={setActiveTab}
      >
        <PageHeader
          title={
            activeTab === "dash"
              ? "Sales Overview"
              : activeTab === "requests"
                ? "Request Approvals"
                : activeTab === "nomination"
                  ? "Truck Nomination"
                  : activeTab === "revisions"
                    ? "Nomination Revisions"
                    : activeTab === "inventory"
                      ? "Fuel Products"
                      : "Customer Directory"
          }
          crumbs={["Sales", activeTab]}
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

        {/* DASHBOARD VIEW */}
        {activeTab === "dash" && (
          <>
            <div className="fef-stat-grid">
              <StatCard
                label="Pending Requests"
                value={stats.pending}
                icon={FiClock}
                tone="accent"
              />
              <StatCard
                label="Approved Requests"
                value={stats.approved}
                icon={FiCheckSquare}
                tone="success"
              />
              <StatCard
                label="Active Customers"
                value={stats.customers}
                icon={FiUsers}
                tone="primary"
              />
            </div>

            <div className="fef-panel" style={{ marginTop: 24 }}>
              <div className="fef-panel-head">
                <h3>Pending Fuel Requests</h3>
              </div>
              <div className="fef-table-wrap">
                <table className="fef-table">
                  <thead>
                    <tr>
                      <th>Ref</th>
                      <th>Customer</th>
                      <th>Product</th>
                      <th>Qty (L)</th>
                      <th>Calculated Amount</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {ordersList
                      .filter((o) => o.orderStatus === "PENDING")
                      .slice(0, 5)
                      .map((o) => {
                        const isEmergency =
                          o.customerName === "Stranded Drivers (Emergency Requests)" ||
                          o.customerName === "Customer Fuel Requests";
                        return (
                          <tr key={o.id}>
                            <td>{o.orderNumber}</td>
                            <td>
                              <div>
                                {isEmergency && o.driverName
                                  ? o.driverName
                                  : o.customerName === "Stranded Drivers (Emergency Requests)"
                                    ? "Customer Fuel Requests"
                                    : o.customerName || "Customer"}
                              </div>
                              {o.emergencyLevel && (
                                <span
                                  className={`fef-badge fef-badge-${o.emergencyLevel.toLowerCase().includes("critical") ? "danger" : "info"}`}
                                  style={{
                                    fontSize: 10,
                                    padding: "2px 6px",
                                    marginTop: 4,
                                    display: "inline-block",
                                  }}
                                >
                                  {o.emergencyLevel}
                                </span>
                              )}
                              {o.driverName && !isEmergency && (
                                <div
                                  style={{
                                    fontSize: 11,
                                    color: "var(--feftms-text-muted)",
                                    marginTop: 4,
                                  }}
                                >
                                  Customer: {o.driverName} ({o.driverPhone})
                                </div>
                              )}
                              {isEmergency && o.driverPhone && (
                                <div
                                  style={{
                                    fontSize: 11,
                                    color: "var(--feftms-text-muted)",
                                    marginTop: 4,
                                  }}
                                >
                                  Phone: {o.driverPhone}
                                </div>
                              )}
                              {o.locationAddress && (
                                <div
                                  style={{
                                    fontSize: 10,
                                    color: "var(--feftms-text-muted)",
                                    marginTop: 2,
                                  }}
                                  title={`GPS: ${o.locationGps || "N/A"} | Landmark: ${o.locationLandmark || "N/A"}`}
                                >
                                  📍 {o.locationAddress}
                                </div>
                              )}
                            </td>
                            <td>{o.productName}</td>
                            <td>{o.quantity?.toLocaleString()}</td>
                            <td>${o.amount?.toLocaleString()}</td>
                            <td>
                              <button
                                className="fef-btn fef-btn-primary"
                                style={{ padding: "6px 12px", fontSize: "13px" }}
                                onClick={() => setSelectedRequest(o)}
                                title="Review request details"
                              >
                                Review
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    {ordersList.filter((o) => o.orderStatus === "PENDING").length === 0 && (
                      <tr>
                        <td
                          colSpan="6"
                          style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                        >
                          No pending requests in database.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

        {/* REQUESTS VIEW */}
        {activeTab === "requests" && (
          <div className="fef-panel">
            <div className="fef-panel-head">
              <h3>All Customer Requests</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Order #</th>
                    <th>Customer</th>
                    <th>Fuel Product</th>
                    <th>Qty (L)</th>
                    <th>Estimated Price</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {ordersList.map((o) => {
                    const isEmergency =
                      o.customerName === "Stranded Drivers (Emergency Requests)" ||
                      o.customerName === "Customer Fuel Requests";
                    return (
                      <tr key={o.id}>
                        <td>{o.orderNumber}</td>
                        <td>
                          <div>
                            {isEmergency && o.driverName
                              ? o.driverName
                              : o.customerName === "Stranded Drivers (Emergency Requests)"
                                ? "Customer Fuel Requests"
                                : o.customerName || "Customer"}
                          </div>
                          {o.emergencyLevel && (
                            <span
                              className={`fef-badge fef-badge-${o.emergencyLevel.toLowerCase().includes("critical") ? "danger" : "info"}`}
                              style={{
                                fontSize: 10,
                                padding: "2px 6px",
                                marginTop: 4,
                                display: "inline-block",
                              }}
                            >
                              {o.emergencyLevel}
                            </span>
                          )}
                          {o.driverName && !isEmergency && (
                            <div
                              style={{
                                fontSize: 11,
                                color: "var(--feftms-text-muted)",
                                marginTop: 4,
                              }}
                            >
                              Customer: {o.driverName} ({o.driverPhone})
                            </div>
                          )}
                          {isEmergency && o.driverPhone && (
                            <div
                              style={{
                                fontSize: 11,
                                color: "var(--feftms-text-muted)",
                                marginTop: 4,
                              }}
                            >
                              Phone: {o.driverPhone}
                            </div>
                          )}
                          {o.locationAddress && (
                            <div
                              style={{
                                fontSize: 10,
                                color: "var(--feftms-text-muted)",
                                marginTop: 2,
                              }}
                              title={`GPS: ${o.locationGps || "N/A"} | Landmark: ${o.locationLandmark || "N/A"}`}
                            >
                              📍 {o.locationAddress}
                            </div>
                          )}
                        </td>
                        <td>{o.productName}</td>
                        <td>{o.quantity?.toLocaleString()} L</td>
                        <td>${o.amount?.toLocaleString()}</td>
                        <td>
                          <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                            <span className={`fef-badge fef-badge-${o.orderStatus?.toLowerCase()}`}>
                              {o.orderStatus}
                            </span>
                            {o.paymentStatus && (
                              <span
                                className={`fef-badge fef-badge-${o.paymentStatus.toLowerCase()}`}
                                style={{ fontSize: "10px", width: "fit-content" }}
                              >
                                {o.paymentStatus === "PENDING_PAYMENT"
                                  ? "Pending Payment"
                                  : o.paymentStatus === "PAID"
                                    ? "Paid"
                                    : o.paymentStatus}
                              </span>
                            )}
                          </div>
                        </td>
                        <td style={{ display: "flex", gap: "6px" }}>
                          <button
                            className={`fef-btn ${o.orderStatus === "PENDING" ? "fef-btn-primary" : "fef-btn-outline"}`}
                            style={{ padding: "6px 12px", fontSize: "13px" }}
                            onClick={() => setSelectedRequest(o)}
                            title={
                              o.orderStatus === "PENDING"
                                ? "Review request details"
                                : "View request details"
                            }
                          >
                            {o.orderStatus === "PENDING" ? "Review" : "View"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TRUCK NOMINATION TAB & EDITOR */}
        {selectedNominationOrder ? (
          <div className="fef-panel">
            {nominationForm.rejectionReason && (
              <div className="fef-alert fef-alert-danger" style={{ marginBottom: 20 }}>
                <strong>⚠️ Operations Revision Comments:</strong>
                <p style={{ margin: "5px 0 0 0", fontSize: 13 }}>{nominationForm.rejectionReason}</p>
              </div>
            )}
            <div>
              <div className="fef-panel-head" style={{ marginBottom: 20, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <h3 style={{ margin: 0 }}>
                    Truck Nomination for Order {selectedNominationOrder.orderNumber}
                  </h3>
                  <p style={{ margin: "4px 0 0 0", fontSize: 13, color: "var(--feftms-text-muted)" }}>
                    Customer: {selectedNominationOrder.customerName} | Product: {selectedNominationOrder.productName} | Total Ordered: {(selectedNominationOrder.approvedQuantity || selectedNominationOrder.quantity)?.toLocaleString()} L
                  </p>
                </div>
                <button
                  className="fef-btn fef-btn-outline"
                  onClick={() => {
                    setSelectedNominationOrder(null);
                    setNominationError("");
                    setNominationSuccess("");
                  }}
                  >
                    Back to List
                  </button>
                </div>

                {nominationLoading ? (
                  <div style={{ padding: 40, textAlign: "center" }}>Loading nomination details...</div>
                ) : (
                  <div>
                    {nominationError && (
                      <div className="fef-alert fef-alert-danger fef-fade-in" style={{ marginBottom: 20 }}>
                        <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
                        {nominationError}
                      </div>
                    )}

                    {nominationSuccess && (
                      <div className="fef-alert fef-alert-success fef-fade-in" style={{ marginBottom: 20 }}>
                        <FiCheckCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
                        {nominationSuccess}
                      </div>
                    )}

                    {/* Check if submitted and locked */}
                    {["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus) && (
                      <div className="fef-alert fef-alert-info" style={{ marginBottom: 20 }}>
                        <FiInfo style={{ verticalAlign: "-2px", marginRight: 6 }} />
                        <strong>Nomination Locked:</strong> This truck nomination has been submitted and is read-only.
                      </div>
                    )}

                    <div className="fef-card" style={{ padding: 20, marginBottom: 20 }}>
                      <h4 style={{ marginTop: 0, marginBottom: 15 }}>Transport Configuration</h4>
                      <div className="fef-form-grid">
                        <div className="fef-field">
                          <label className="fef-label">Customer Transport</label>
                          <select
                            className="fef-select"
                            value={nominationForm.transportSource}
                            onChange={(e) => handleTransportSourceChange(e.target.value)}
                            disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                          >
                            <option value="CUSTOMER_TRUCKS">Yes (Customer Own Transport)</option>
                            <option value="FALCON_ARRANGED">No (Falcon Arranged Transport)</option>
                          </select>
                        </div>

                        {nominationForm.transportSource === "CUSTOMER_TRUCKS" && (
                          <div className="fef-field">
                            <label className="fef-label">Number of Trucks</label>
                            <input
                              type="number"
                              required
                              placeholder="e.g. 3"
                              className="fef-input"
                              value={nominationForm.numberOfTrucks}
                              onChange={(e) => handleNumberOfTrucksChange(e.target.value)}
                              disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                            />
                          </div>
                        )}
                      </div>

                      <div className="fef-field" style={{ marginTop: 15 }}>
                        <label className="fef-label">Confirmation & Logistics Notes</label>
                        <textarea
                          placeholder="Logistics coordinates, driver instructions, etc. (Optional)"
                          className="fef-input"
                          rows="3"
                          value={nominationForm.confirmationNotes}
                          onChange={(e) => setNominationForm({ ...nominationForm, confirmationNotes: e.target.value })}
                          disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                        />
                      </div>
                    </div>

                    {nominationForm.transportSource === "CUSTOMER_TRUCKS" && (
                      <div className="fef-card" style={{ padding: 20 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 15 }}>
                        <h4 style={{ margin: 0 }}>Registered Nomination Trucks</h4>
                        {!["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus) && (
                          <button
                            type="button"
                            className="fef-btn fef-btn-outline"
                            onClick={handleAddTruckItem}
                          >
                            + Add Truck
                          </button>
                        )}
                      </div>

                      {/* Display quantities allocation tracker */}
                      {(() => {
                        const orderQty = selectedNominationOrder.approvedQuantity || selectedNominationOrder.quantity || 0;
                        const totalAllocated = nominationForm.items.reduce((sum, item) => sum + parseFloat(item.allocatedQuantity || 0), 0);
                        const remaining = orderQty - totalAllocated;
                        return (
                          <div style={{ marginBottom: 15, padding: 12, background: "rgba(255,255,255,0.05)", borderRadius: 6, display: "flex", justifyContent: "space-between", fontSize: 13 }}>
                            <div>Total Ordered: <strong>{orderQty.toLocaleString()} L</strong></div>
                            <div>Total Allocated: <strong style={{ color: totalAllocated === orderQty ? "#10B981" : "#F59E0B" }}>{totalAllocated.toLocaleString()} L</strong></div>
                            <div>Remaining: <strong style={{ color: remaining === 0 ? "#10B981" : "#EF4444" }}>{remaining.toLocaleString()} L</strong></div>
                          </div>
                        );
                      })()}

                      {nominationForm.items.length === 0 ? (
                        <p style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: "20px 0" }}>
                          No trucks registered. Add at least one truck to complete this nomination.
                        </p>
                      ) : (
                        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
                          {nominationForm.items.map((item, idx) => (
                            <div key={idx} style={{ padding: 15, background: "rgba(0,0,0,0.2)", borderRadius: 6, border: "1px solid rgba(255,255,255,0.05)" }}>
                              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                                <strong style={{ fontSize: 13, color: "var(--feftms-primary-light)" }}>Truck #{idx + 1}</strong>
                                {!["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus) && (
                                  <button
                                    type="button"
                                    onClick={() => handleRemoveTruckItem(idx)}
                                    style={{ background: "none", border: "none", color: "#EF4444", cursor: "pointer", fontSize: 12 }}
                                  >
                                    Remove Truck
                                  </button>
                                )}
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
                                        handleUpdateTruckItem(idx, "truckNumber", veh.plateNumber);
                                        handleUpdateTruckItem(idx, "truckCapacity", veh.capacity);
                                        if (veh.driver) {
                                          handleUpdateTruckItem(idx, "driverName", `${veh.driver.firstName} ${veh.driver.lastName}`);
                                          handleUpdateTruckItem(idx, "driverLicenceNumber", veh.driver.licenseNumber);
                                        }
                                      }
                                    }}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  >
                                    <option value="">-- Choose Registered Vehicle --</option>
                                    {vehiclesList.map(v => (
                                      <option key={v.id} value={v.id}>{v.plateNumber} (Cap: {v.capacity}L)</option>
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
                                        handleUpdateTruckItem(idx, "driverName", `${drv.firstName} ${drv.lastName}`);
                                        handleUpdateTruckItem(idx, "driverLicenceNumber", drv.licenseNumber);
                                      }
                                    }}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
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
                                    value={item.truckNumber}
                                    onChange={(e) => handleUpdateTruckItem(idx, "truckNumber", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Trailer Plate Number</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={item.trailerNumber}
                                    onChange={(e) => handleUpdateTruckItem(idx, "trailerNumber", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Driver Name</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={item.driverName}
                                    onChange={(e) => handleUpdateTruckItem(idx, "driverName", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Driver Licence Number</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={item.driverLicenceNumber}
                                    onChange={(e) => handleUpdateTruckItem(idx, "driverLicenceNumber", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Passport / ID Number</label>
                                  <input
                                    className="fef-input"
                                    value={item.driverPassport || ""}
                                    onChange={(e) => handleUpdateTruckItem(idx, "driverPassport", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Transport Company</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={item.transportCompany}
                                    onChange={(e) => handleUpdateTruckItem(idx, "transportCompany", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Destination</label>
                                  <input
                                    required
                                    className="fef-input"
                                    value={item.destination}
                                    onChange={(e) => handleUpdateTruckItem(idx, "destination", e.target.value)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Truck Capacity (L)</label>
                                  <input
                                    type="number"
                                    required
                                    className="fef-input"
                                    value={item.truckCapacity || ""}
                                    onChange={(e) => handleUpdateTruckItem(idx, "truckCapacity", e.target.value ? parseFloat(e.target.value) : 0)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                                <div className="fef-field">
                                  <label className="fef-label">Allocated Quantity (L)</label>
                                  <input
                                    type="number"
                                    required
                                    className="fef-input"
                                    value={item.allocatedQuantity || ""}
                                    onChange={(e) => handleUpdateTruckItem(idx, "allocatedQuantity", e.target.value ? parseFloat(e.target.value) : 0)}
                                    disabled={["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus)}
                                  />
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                    {!["READY_FOR_LOADING", "LOADING_ORDER_CREATED", "LOADING_ORDER_APPROVED", "LOADING_IN_PROGRESS", "LOADING_COMPLETED"].includes(selectedNominationOrder.orderStatus) && (
                      <div style={{ marginTop: 25, display: "flex", gap: 10 }}>
                        <button
                          type="button"
                          className="fef-btn fef-btn-primary"
                          onClick={handleSaveNominationDraft}
                          disabled={nominationLoading}
                        >
                          Save Draft
                        </button>
                        <button
                          type="button"
                          className="fef-btn fef-btn-primary"
                          onClick={handleSubmitNomination}
                          disabled={nominationLoading || (nominationForm.transportSource === "CUSTOMER_TRUCKS" && nominationForm.items.length === 0)}
                          style={{ background: "#10B981" }}
                        >
                          Submit Nomination
                        </button>
                        <button
                          type="button"
                          className="fef-btn fef-btn-outline"
                          onClick={() => {
                            setSelectedNominationOrder(null);
                            setNominationError("");
                            setNominationSuccess("");
                          }}
                        >
                          Close View
                        </button>
                        {nominationForm.id && (
                          <button
                            type="button"
                            className="fef-btn fef-btn-outline"
                            onClick={handleCancelNominationClick}
                            disabled={nominationLoading}
                            style={{ color: "#EF4444", borderColor: "#EF4444", marginLeft: "auto" }}
                          >
                            Cancel Nomination
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <>
            {activeTab === "nomination" && (
              <div className="fef-panel">
                <div>
                  <div className="fef-panel-head">
                    <h3>Approved Paid Orders Awaiting Truck Nomination</h3>
                  </div>
                  <div className="fef-table-wrap">
                    <table className="fef-table">
                      <thead>
                        <tr>
                          <th>Order #</th>
                          <th>Customer</th>
                          <th>Product</th>
                          <th>Quantity (L)</th>
                          <th>Order Status</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {ordersList
                          .filter(o => o.paymentStatus === "PAID" && o.orderStatus !== "REVISION_REQUESTED")
                          .map(o => (
                            <tr key={o.id}>
                              <td>{o.orderNumber}</td>
                              <td>{o.customerName}</td>
                              <td>{o.productName}</td>
                              <td>{(o.approvedQuantity || o.quantity)?.toLocaleString()}</td>
                              <td>
                                <span className={`fef-badge fef-badge-${o.orderStatus?.toLowerCase()}`}>
                                  {o.orderStatus?.replace(/_/g, " ")}
                                </span>
                              </td>
                              <td>
                                <button
                                  className="fef-btn fef-btn-primary"
                                  onClick={() => handleOpenNomination(o)}
                                >
                                  {o.orderStatus === "PAYMENT_CONFIRMED" ? "Create Nomination" : "Manage Nomination"}
                                </button>
                              </td>
                            </tr>
                          ))}
                        {ordersList.filter(o => o.paymentStatus === "PAID" && o.orderStatus !== "REVISION_REQUESTED").length === 0 && (
                          <tr>
                            <td colSpan="6" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 20 }}>
                              No paid orders awaiting truck nomination.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* REVISIONS REQUESTED TAB */}
            {activeTab === "revisions" && (
              <div className="fef-panel">
                <div className="fef-panel-head" style={{ marginBottom: 15 }}>
                  <h3>Truck Nominations requiring Revisions</h3>
                  <span className="fef-badge fef-badge-danger" style={{ fontSize: 12 }}>
                    Pending Corrections: {ordersList.filter(o => o.paymentStatus === "PAID" && o.orderStatus === "REVISION_REQUESTED").length}
                  </span>
                </div>
                <div className="fef-table-wrap">
                  <table className="fef-table">
                    <thead>
                      <tr>
                        <th>Order #</th>
                        <th>Customer</th>
                        <th>Product</th>
                        <th>Quantity (L)</th>
                        <th>Order Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {ordersList
                        .filter(o => o.paymentStatus === "PAID" && o.orderStatus === "REVISION_REQUESTED")
                        .map(o => (
                          <tr key={o.id}>
                            <td>{o.orderNumber}</td>
                            <td>{o.customerName}</td>
                            <td>{o.productName}</td>
                            <td>{(o.approvedQuantity || o.quantity)?.toLocaleString()} L</td>
                            <td>
                              <span className="fef-badge fef-badge-danger" style={{ background: "#FEF2F2", color: "#EF4444", border: "1px solid rgba(239, 68, 68, 0.2)" }}>
                                Revision Requested
                              </span>
                            </td>
                            <td>
                              <button
                                className="fef-btn fef-btn-primary"
                                onClick={() => handleOpenNomination(o)}
                                style={{ background: "#EF4444" }}
                              >
                                Edit Nomination
                              </button>
                            </td>
                          </tr>
                        ))}
                      {ordersList.filter(o => o.paymentStatus === "PAID" && o.orderStatus === "REVISION_REQUESTED").length === 0 && (
                        <tr>
                          <td colSpan="6" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 20 }}>
                            No truck nominations currently require revisions.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}

        {/* FUEL INVENTORY VIEW */}
        {activeTab === "inventory" && (
          <div className="fef-panel">
            <div className="fef-panel-head">
              <h3>Fuel Inventory & Pricing Reference</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Product Name</th>
                    <th>Fuel Type</th>
                    <th>Selling Price</th>
                    <th>Density</th>
                    <th>Available Quantity</th>
                    <th>Availability Status</th>
                  </tr>
                </thead>
                <tbody>
                  {productsList.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <strong>{p.productName}</strong>
                      </td>
                      <td>{p.fuelType}</td>
                      <td style={{ color: "var(--feftms-primary)", fontWeight: 700 }}>
                        ${p.unitPrice?.toFixed(2)}/L
                      </td>
                      <td>{p.density} kg/L</td>
                      <td>{p.availableQuantity?.toLocaleString()} Liters</td>
                      <td>
                        <span
                          className={`fef-badge fef-badge-${p.status?.toLowerCase() === "active" || p.status?.toLowerCase() === "available" ? "success" : "danger"}`}
                        >
                          {p.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {productsList.length === 0 && (
                    <tr>
                      <td
                        colSpan="6"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No fuel products found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* CUSTOMERS VIEW */}
        {activeTab === "customers" && (
          <div className="fef-panel">
            <div className="fef-panel-head" style={{ marginBottom: 20 }}>
              <h3>Registered Customer Base</h3>
              <button className="fef-btn fef-btn-primary" onClick={() => setShowModal(true)}>
                <FiPlus /> Register Customer
              </button>
            </div>

            {showModal && (
              <div
                className="fef-card"
                style={{ padding: 20, marginBottom: 24, background: "rgba(255,255,255,0.05)" }}
              >
                <h4>Register New Customer</h4>
                <form onSubmit={handleCreateCustomer} style={{ marginTop: 15 }}>
                  <div className="fef-form-grid">
                    <div className="fef-field">
                      <label className="fef-label">Company Name</label>
                      <input
                        required
                        className="fef-input"
                        value={custForm.companyName}
                        onChange={(e) => setCustForm({ ...custForm, companyName: e.target.value })}
                        placeholder="Acme Logistics Ltd"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Contact Person</label>
                      <input
                        required
                        className="fef-input"
                        value={custForm.contactPerson}
                        onChange={(e) =>
                          setCustForm({ ...custForm, contactPerson: e.target.value })
                        }
                        placeholder="John Doe"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Phone Number</label>
                      <input
                        className="fef-input"
                        value={custForm.phone}
                        onChange={(e) => setCustForm({ ...custForm, phone: e.target.value })}
                        placeholder="+254 700 000000"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Email Address</label>
                      <input
                        type="email"
                        className="fef-input"
                        value={custForm.email}
                        onChange={(e) => setCustForm({ ...custForm, email: e.target.value })}
                        placeholder="billing@acme.com"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Physical Address</label>
                      <input
                        className="fef-input"
                        value={custForm.address}
                        onChange={(e) => setCustForm({ ...custForm, address: e.target.value })}
                        placeholder="Depot 4, Mombasa Road, Nairobi"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">TIN / KRA PIN</label>
                      <input
                        className="fef-input"
                        value={custForm.tinNumber}
                        onChange={(e) => setCustForm({ ...custForm, tinNumber: e.target.value })}
                        placeholder="P051234567Z"
                      />
                    </div>
                    <div className="fef-field">
                      <label className="fef-label">Customer Code (Optional)</label>
                      <input
                        className="fef-input"
                        value={custForm.customerCode}
                        onChange={(e) => setCustForm({ ...custForm, customerCode: e.target.value })}
                        placeholder="ACME01"
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
                      onClick={() => setShowModal(false)}
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
                    <th>Code</th>
                    <th>Company Name</th>
                    <th>Contact Person</th>
                    <th>Phone</th>
                    <th>Email</th>
                    <th>Address</th>
                    <th>TIN / PIN</th>
                  </tr>
                </thead>
                <tbody>
                  {customersList.map((c) => (
                    <tr key={c.id}>
                      <td>
                        <strong>{c.customerCode}</strong>
                      </td>
                      <td>{c.companyName}</td>
                      <td>{c.contactPerson}</td>
                      <td>{c.phone || "—"}</td>
                      <td>{c.email || "—"}</td>
                      <td>{c.address || "—"}</td>
                      <td>{c.tinNumber || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Detail / Review Modal */}
        {selectedRequest &&
          (() => {
            if (typeof window === "undefined") return null;
            const prod = productsList.find(
              (p) =>
                p.id === selectedRequest.product?.id ||
                p.productName === selectedRequest.productName,
            );
            const availableStock = prod ? prod.availableQuantity : 0;
            const isStockLow =
              selectedRequest.orderStatus === "PENDING" &&
              availableStock < selectedRequest.quantity;

            return createPortal(
              <div
                className="fef-modal-backdrop"
                onClick={() => {
                  setSelectedRequest(null);
                  setIsEditing(false);
                  setEditQty("");
                  setEditReason("");
                  setValidationError("");
                }}
              >
                <div
                  className="fef-modal-window"
                  style={{ maxWidth: "650px" }}
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    className="fef-modal-close"
                    onClick={() => {
                      setSelectedRequest(null);
                      setIsEditing(false);
                      setEditQty("");
                      setEditReason("");
                      setValidationError("");
                    }}
                    title="Close"
                  >
                    <FiX />
                  </button>

                  <div className="fef-detail-modal-header">
                    <div className="fef-detail-modal-badges">
                      <span className="fef-badge fef-badge-pending" style={{ fontSize: "11px" }}>
                        {selectedRequest.orderNumber}
                      </span>
                      <span
                        className={`fef-badge fef-badge-${selectedRequest.orderStatus?.toLowerCase()}`}
                      >
                        {selectedRequest.orderStatus}
                      </span>
                      {selectedRequest.emergencyLevel && (
                        <span
                          className={`fef-badge fef-badge-${selectedRequest.emergencyLevel.toLowerCase().includes("critical") ? "danger" : "info"}`}
                        >
                          {selectedRequest.emergencyLevel} Priority
                        </span>
                      )}
                      {selectedRequest.paymentStatus && (
                        <span
                          className={`fef-badge fef-badge-${selectedRequest.paymentStatus.toLowerCase()}`}
                          style={{ fontSize: "11px" }}
                        >
                          Payment:{" "}
                          {selectedRequest.paymentStatus === "PENDING_PAYMENT"
                            ? "Pending"
                            : selectedRequest.paymentStatus === "PAID"
                              ? "Paid"
                              : selectedRequest.paymentStatus}
                        </span>
                      )}
                    </div>
                    <h2 className="fef-detail-modal-title">Fuel Delivery Request Details</h2>
                  </div>

                  <div className="fef-detail-modal-body">
                    {/* Insufficient Stock Warning */}
                    {isStockLow && (
                      <div
                        className="fef-alert fef-alert-danger fef-fade-in"
                        style={{
                          marginBottom: 20,
                          display: "flex",
                          gap: "10px",
                          alignItems: "center",
                        }}
                      >
                        <FiAlertCircle size={20} style={{ flexShrink: 0 }} />
                        <div>
                          <strong>⚠️ Stock Alert:</strong> Customer requested quantity (
                          <strong>{selectedRequest.quantity?.toLocaleString()} L</strong>) exceeds
                          available stock (<strong>{availableStock?.toLocaleString()} L</strong>).
                        </div>
                      </div>
                    )}

                    {/* Customer & Driver Info */}
                    <div className="fef-detail-section">
                      <h4 className="fef-detail-section-title">Customer Details</h4>
                      <div className="fef-detail-grid">
                        {selectedRequest.customerName &&
                          selectedRequest.customerName !==
                            "Stranded Drivers (Emergency Requests)" &&
                          selectedRequest.customerName !== "Customer Fuel Requests" && (
                            <div className="fef-detail-item">
                              <span className="fef-detail-label">Account / Customer</span>
                              <span className="fef-detail-value">
                                {selectedRequest.customerName}
                              </span>
                            </div>
                          )}
                        {selectedRequest.driverName && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Company Name</span>
                            <span className="fef-detail-value">{selectedRequest.driverName}</span>
                          </div>
                        )}
                        {selectedRequest.driverPhone && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Customer Phone</span>
                            <span className="fef-detail-value">
                              <a href={`tel:${selectedRequest.driverPhone}`}>
                                {selectedRequest.driverPhone}
                              </a>
                            </span>
                          </div>
                        )}
                        {selectedRequest.driverEmail && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Customer Email</span>
                            <span className="fef-detail-value" style={{ fontWeight: "normal" }}>
                              {selectedRequest.driverEmail}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Fuel Order Details */}
                    <div className="fef-detail-section">
                      <h4 className="fef-detail-section-title">Fuel Order Details</h4>
                      <div className="fef-detail-grid">
                        <div className="fef-detail-item">
                          <span className="fef-detail-label">Fuel Product</span>
                          <span className="fef-detail-value">{selectedRequest.productName}</span>
                        </div>
                        <div className="fef-detail-item">
                          <span className="fef-detail-label">Quantity</span>
                          {isEditing ? (
                            <input
                              type="number"
                              className={`fef-order-edit-input ${validationError ? "fef-input-invalid" : ""}`}
                              placeholder="e.g. 5000"
                              value={editQty}
                              onChange={(e) => {
                                setEditQty(e.target.value);
                                setValidationError("");
                              }}
                            />
                          ) : (
                            <span className="fef-detail-value">
                              {selectedRequest.quantity?.toLocaleString()} L
                              {selectedRequest.originalQuantity && (
                                <span
                                  style={{
                                    fontSize: "11px",
                                    display: "block",
                                    color: "var(--feftms-text-muted)",
                                  }}
                                >
                                  (Originally requested:{" "}
                                  {selectedRequest.originalQuantity?.toLocaleString()} L)
                                </span>
                              )}
                            </span>
                          )}
                        </div>
                        <div className="fef-detail-item">
                          <span className="fef-detail-label">Calculated Price</span>
                          <span
                            className="fef-detail-value"
                            style={{ color: "var(--feftms-primary)", fontSize: "16px" }}
                          >
                            $
                            {isEditing
                              ? (parseFloat(editQty || 0) * (prod?.unitPrice || 0)).toLocaleString(
                                  undefined,
                                  { minimumFractionDigits: 2, maximumFractionDigits: 2 },
                                )
                              : selectedRequest.amount?.toLocaleString()}
                          </span>
                        </div>
                        {selectedRequest.vehicleType && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Vehicle Type</span>
                            <span
                              className="fef-detail-value"
                              style={{ textTransform: "capitalize" }}
                            >
                              {selectedRequest.vehicleType}
                            </span>
                          </div>
                        )}
                        {selectedRequest.paymentMethod && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Payment Method</span>
                            <span className="fef-detail-value">
                              {selectedRequest.paymentMethod}
                            </span>
                          </div>
                        )}
                        {selectedRequest.paymentStatus && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Payment Status</span>
                            <span className="fef-detail-value">
                              <span
                                className={`fef-badge fef-badge-${selectedRequest.paymentStatus.toLowerCase()}`}
                                style={{ padding: "2px 8px", fontSize: "12px" }}
                              >
                                {selectedRequest.paymentStatus === "PENDING_PAYMENT"
                                  ? "Pending Payment"
                                  : selectedRequest.paymentStatus === "PAID"
                                    ? "Paid"
                                    : selectedRequest.paymentStatus}
                              </span>
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Inline Editing details / history */}
                    {isEditing && (
                      <div
                        className="fef-detail-section"
                        style={{ borderTop: "1px dashed rgba(255,255,255,0.1)", paddingTop: 15 }}
                      >
                        <h4 className="fef-detail-section-title">Order Modification History Log</h4>
                        <div className="fef-field" style={{ marginBottom: 10 }}>
                          <label
                            className="fef-label"
                            style={{ fontSize: "12px", color: "var(--feftms-text-muted)" }}
                          >
                            Reason for edit (Recommended)
                          </label>
                          <textarea
                            className="fef-order-edit-input"
                            style={{ height: "60px", resize: "none" }}
                            value={editReason}
                            onChange={(e) => setEditReason(e.target.value)}
                            placeholder="e.g. Stock limitations require quantity reduction."
                          />
                        </div>
                        {validationError && (
                          <div
                            style={{
                              color: "#ef4444",
                              fontSize: "12px",
                              fontWeight: "600",
                              marginTop: "4px",
                            }}
                          >
                            ⚠️ {validationError}
                          </div>
                        )}
                      </div>
                    )}

                    {/* Delivery Location */}
                    <div className="fef-detail-section">
                      <h4 className="fef-detail-section-title">Delivery Location</h4>
                      <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                        <div className="fef-detail-item">
                          <span className="fef-detail-label">Address</span>
                          <span className="fef-detail-value">
                            📍 {selectedRequest.locationAddress || "Not Provided"}
                          </span>
                        </div>
                        {selectedRequest.locationLandmark && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">Nearby Landmark</span>
                            <span className="fef-detail-value" style={{ fontWeight: "normal" }}>
                              {selectedRequest.locationLandmark}
                            </span>
                          </div>
                        )}
                        {selectedRequest.locationGps && (
                          <div className="fef-detail-item">
                            <span className="fef-detail-label">GPS Coordinates</span>
                            <div className="fef-detail-gps-row">
                              <code>{selectedRequest.locationGps}</code>
                              <a
                                href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(selectedRequest.locationGps)}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="fef-btn fef-btn-outline"
                                style={{
                                  padding: "4px 10px",
                                  fontSize: "12px",
                                  borderRadius: "8px",
                                }}
                              >
                                Show on Google Maps
                              </a>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Special Instructions */}
                    {selectedRequest.notes && (
                      <div className="fef-detail-section">
                        <h4 className="fef-detail-section-title">Special Instructions</h4>
                        <p className="fef-detail-notes">"{selectedRequest.notes}"</p>
                      </div>
                    )}

                    {/* Actions */}
                    <div className="fef-detail-actions">
                      {selectedRequest.orderStatus === "PENDING" ? (
                        <>
                          {isStockLow ? (
                            <>
                              {isEditing ? (
                                <>
                                  <button
                                    className="fef-btn fef-btn-outline"
                                    onClick={() => {
                                      setIsEditing(false);
                                      setValidationError("");
                                    }}
                                    style={{ padding: "10px 20px" }}
                                  >
                                    Cancel Edit
                                  </button>
                                  <button
                                    className="fef-btn fef-btn-success"
                                    onClick={() => handleApproveEdited(selectedRequest.id)}
                                    style={{ padding: "10px 20px" }}
                                  >
                                    <FiCheck /> Approve Order
                                  </button>
                                </>
                              ) : (
                                <>
                                  <button
                                    className="fef-btn fef-btn-danger"
                                    onClick={() => {
                                      handleReject(selectedRequest.id);
                                      setSelectedRequest(null);
                                    }}
                                    style={{ padding: "10px 20px" }}
                                  >
                                    <FiX /> Reject Order
                                  </button>
                                  <button
                                    className="fef-btn fef-btn-primary"
                                    onClick={() => {
                                      setIsEditing(true);
                                      setEditQty(selectedRequest.quantity.toString());
                                      setEditReason("");
                                    }}
                                    style={{ padding: "10px 20px" }}
                                  >
                                    <FiEdit style={{ marginRight: 4 }} /> Edit Order
                                  </button>
                                </>
                              )}
                            </>
                          ) : (
                            <>
                              <button
                                className="fef-btn fef-btn-danger"
                                onClick={() => {
                                  handleReject(selectedRequest.id);
                                  setSelectedRequest(null);
                                }}
                                style={{ padding: "10px 20px" }}
                              >
                                <FiX /> Reject Request
                              </button>
                              <button
                                className="fef-btn fef-btn-success"
                                onClick={() => {
                                  handleApprove(selectedRequest.id);
                                  setSelectedRequest(null);
                                }}
                                style={{ padding: "10px 20px" }}
                              >
                                <FiCheck /> Approve Request
                              </button>
                            </>
                          )}
                        </>
                      ) : (
                        <div style={{ display: "flex", gap: "10px", width: "100%" }}>
                          <button
                            className="fef-btn fef-btn-outline"
                            onClick={() => setSelectedRequest(null)}
                            style={{ padding: "10px 20px", flex: 1 }}
                          >
                            Close
                          </button>
                          {selectedRequest.invoiceId && (
                            <button
                              className="fef-btn fef-btn-primary"
                              onClick={() => handleViewInvoice(selectedRequest.invoiceId)}
                              style={{ padding: "10px 20px", flex: 1 }}
                            >
                              <FiFileText style={{ marginRight: "4px", verticalAlign: "-2px" }} />{" "}
                              View Invoice
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>,
              document.body,
            );
          })()}
        {selectedInvoice && (
          <InvoiceModal
            invoice={selectedInvoice}
            onClose={() => setSelectedInvoice(null)}
            onRefresh={loadData}
            userRole="SALES_OFFICER"
          />
        )}
      </DashboardLayout>
    </RouteGuard>
  );
}
