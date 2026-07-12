import { useState, useEffect } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { createPortal } from "react-dom";
import {
  FiHome,
  FiDollarSign,
  FiFileText,
  FiX,
  FiCheck,
  FiAlertCircle,
  FiCheckCircle,
  FiDroplet,
  FiActivity,
  FiCreditCard,
  FiSettings,
} from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import { RouteGuard } from "../components/RouteGuard";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { listProducts, updateProduct } from "../services/productService";
import { listOrders } from "../services/orderService";
import { listInvoices } from "../services/invoiceService";
import { InvoiceModal } from "../components/InvoiceModal";
import {
  listPaymentAccounts,
  createPaymentAccount,
  updatePaymentAccount,
  deletePaymentAccount,
  togglePaymentAccountStatus,
} from "../services/paymentAccountService";
import { getCompanySettings, updateCompanySettings } from "../services/companySettingsService";

export const Route = createFileRoute("/finance")({
  head: () => ({ meta: [{ title: "Finance Desk — FEFTMS" }] }),
  component: FinanceDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "pricing", label: "Fuel Pricing", icon: FiDollarSign },
  { key: "invoices", label: "Invoices", icon: FiFileText },
  { key: "paymentAccounts", label: "Payment Accounts", icon: FiCreditCard },
  { key: "companySettings", label: "Company Settings", icon: FiSettings },
];

function FinanceDash() {
  const [activeTab, setActiveTab] = useState("dash");
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [paymentAccounts, setPaymentAccounts] = useState([]);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modals
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [newPrice, setNewPrice] = useState("");

  // Company Settings Form State
  const [companySettings, setCompanySettings] = useState({
    companyName: "",
    postalAddress: "",
    officeAddress: "",
    phoneNumber: "",
    email: "",
    logo: "",
    signatoryName: "",
    signatoryTitle: "",
    signatorySignature: "",
    stamp: "",
  });
  const [savingSettings, setSavingSettings] = useState(false);

  // Payment Account Modals and Form State
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [editingAccount, setEditingAccount] = useState(null);
  const [accountForm, setAccountForm] = useState({
    paymentMethod: "Bank Transfer",
    beneficiaryName: "FALCON ENERGY LIMITED",
    bankName: "",
    branchName: "",
    accountNumber: "",
    swiftCode: "",
    currency: "USD",
    paymentTerms: "PREPAYMENT",
    paymentInstructions: "",
    validityDays: 30,
    status: "ACTIVE",
  });

  const loadProducts = () => {
    setLoading(true);
    setError("");
    listProducts({ size: 100 })
      .then((res) => {
        setProducts(res.content || []);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Failed to load fuel products. Database connection error.");
        setLoading(false);
      });
  };

  const loadInvoices = () => {
    listInvoices({ size: 100 })
      .then((res) => {
        setInvoices(res.content || []);
      })
      .catch((err) => {
        console.warn("Failed to load invoices for finance dashboard", err);
      });
  };

  const loadPaymentAccountsData = () => {
    listPaymentAccounts({ size: 100 })
      .then((res) => {
        setPaymentAccounts(res.content || []);
      })
      .catch((err) => {
        console.error("Failed to load payment accounts", err);
      });
  };

  const loadCompanySettings = () => {
    getCompanySettings()
      .then((res) => {
        setCompanySettings(res.data || res || {});
      })
      .catch((err) => {
        console.error("Failed to load company settings", err);
      });
  };

  const handleSaveCompanySettings = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    setSavingSettings(true);
    try {
      await updateCompanySettings(companySettings);
      setSuccess("Company global settings updated successfully.");
      loadCompanySettings();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to update company settings.");
    } finally {
      setSavingSettings(false);
    }
  };

  const loadData = () => {
    loadProducts();
    loadInvoices();
    loadPaymentAccountsData();
    loadCompanySettings();
  };

  useEffect(() => {
    loadProducts();
    if (activeTab === "dash" || activeTab === "invoices") {
      const endDate = new Date().toISOString();
      const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();
      listOrders({ startDate, endDate, size: 200 })
        .then((res) => {
          setOrders(res.content || []);
        })
        .catch((err) => {
          console.warn("Failed to load orders for finance dashboard", err);
        });
      loadInvoices();
    }
    if (activeTab === "paymentAccounts") {
      loadPaymentAccountsData();
    }
    if (activeTab === "companySettings") {
      loadCompanySettings();
    }
  }, [activeTab]);

  const handleEditPriceClick = (prod) => {
    setSelectedProduct(prod);
    setNewPrice(prod.unitPrice.toString());
    setShowEditModal(true);
  };

  const handleSavePrice = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    if (!selectedProduct) return;

    try {
      const price = parseFloat(newPrice);
      if (isNaN(price) || price <= 0) {
        throw new Error("Price must be a positive number.");
      }

      await updateProduct(selectedProduct.id, {
        productName: selectedProduct.productName,
        fuelType: selectedProduct.fuelType,
        unitPrice: price,
        density: selectedProduct.density,
        availableQuantity: selectedProduct.availableQuantity,
        status: selectedProduct.status,
      });

      setSuccess(`Unit price for ${selectedProduct.productName} updated successfully.`);
      setShowEditModal(false);
      loadProducts();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to update product price.");
    }
  };

  // Payment Account Handlers
  const handleOpenAccountModal = (acc = null) => {
    setError("");
    setSuccess("");
    if (acc) {
      setEditingAccount(acc);
      setAccountForm({
        paymentMethod: acc.paymentMethod || "Bank Transfer",
        beneficiaryName: acc.beneficiaryName || "FALCON ENERGY LIMITED",
        bankName: acc.bankName || "",
        branchName: acc.branchName || "",
        accountNumber: acc.accountNumber || "",
        swiftCode: acc.swiftCode || "",
        currency: acc.currency || "USD",
        paymentTerms: acc.paymentTerms || "PREPAYMENT",
        paymentInstructions: acc.paymentInstructions || "",
        validityDays: acc.validityDays !== undefined ? acc.validityDays : 30,
        status: acc.status || "ACTIVE",
      });
    } else {
      setEditingAccount(null);
      setAccountForm({
        paymentMethod: "Bank Transfer",
        beneficiaryName: "FALCON ENERGY LIMITED",
        bankName: "",
        branchName: "",
        accountNumber: "",
        swiftCode: "",
        currency: "USD",
        paymentTerms: "PREPAYMENT",
        paymentInstructions: "",
        validityDays: 30,
        status: "ACTIVE",
      });
    }
    setShowAccountModal(true);
  };

  const handleSaveAccount = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      if (editingAccount) {
        await updatePaymentAccount(editingAccount.id, accountForm);
        setSuccess("Payment account updated successfully.");
      } else {
        await createPaymentAccount(accountForm);
        setSuccess("Payment account created successfully.");
      }
      setShowAccountModal(false);
      loadPaymentAccountsData();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to save payment account.");
    }
  };

  const handleToggleAccountStatus = async (id) => {
    setError("");
    setSuccess("");
    try {
      await togglePaymentAccountStatus(id);
      setSuccess("Payment account status toggled successfully.");
      loadPaymentAccountsData();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to toggle status.");
    }
  };

  const handleDeleteAccount = async (id) => {
    if (!window.confirm("Are you sure you want to permanently delete this payment account?"))
      return;
    setError("");
    setSuccess("");
    try {
      await deletePaymentAccount(id);
      setSuccess("Payment account deleted successfully.");
      loadPaymentAccountsData();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to delete payment account.");
    }
  };

  // Stats helpers
  const totalProducts = products.length;
  const avgPrice =
    totalProducts > 0
      ? (products.reduce((acc, p) => acc + p.unitPrice, 0) / totalProducts).toFixed(2)
      : "0.00";
  const totalStock = products.reduce((acc, p) => acc + (p.availableQuantity || 0), 0);

  const approvedOrders = orders.filter(
    (o) => o.orderStatus === "APPROVED" || o.orderStatus === "DELIVERED",
  );
  const recentRevenue = approvedOrders.reduce((acc, o) => acc + (o.amount || 0), 0);
  const recentOrdersCount = orders.length;

  return (
    <RouteGuard allowedRoles={["FINANCE"]}>
      <DashboardLayout
        role="FINANCE"
        userName="Sarah Finance"
        pageTitle="Finance Desk"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={setActiveTab}
      >
        <PageHeader
          title={
            activeTab === "dash"
              ? "Finance Console"
              : activeTab === "pricing"
                ? "Fuel Price Control"
                : activeTab === "paymentAccounts"
                  ? "Payment Accounts"
                  : activeTab.charAt(0).toUpperCase() + activeTab.slice(1)
          }
          crumbs={["Finance", activeTab]}
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

        {/* Stats row */}
        <div className="fef-stat-grid">
          <StatCard label="Fuel Products" value={totalProducts} icon={FiDroplet} tone="primary" />
          <StatCard
            label="Average Price/L"
            value={`$${avgPrice}`}
            icon={FiDollarSign}
            tone="accent"
          />
          <StatCard
            label="System Stock Volume"
            value={`${totalStock.toLocaleString()} L`}
            icon={FiActivity}
            tone="success"
          />
          <StatCard
            label="Revenue (30d)"
            value={`$${recentRevenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
            icon={FiCreditCard}
            tone="primary"
          />
          <StatCard label="Sales (30d)" value={recentOrdersCount} icon={FiFileText} tone="accent" />
        </div>

        {/* DASHBOARD TAB */}
        {activeTab === "dash" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Recent Financial Ledger</h3>
            </div>
            <div style={{ padding: 20, color: "var(--feftms-text-muted)" }}>
              Finance users manage fuel pricing in real-time. Use the **Fuel Pricing** tab to
              control pricing variables and synchronize changes across all dashboards. Configure
              system bank accounts using **Payment Accounts** tab.
            </div>
          </div>
        )}

        {/* PRICING TAB */}
        {activeTab === "pricing" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head" style={{ marginBottom: 20 }}>
              <div>
                <h3>Active Fuel Prices & Stock Levels</h3>
                <p style={{ margin: 0, color: "var(--feftms-text-muted)" }}>
                  Fuel products are created by the operations team. Finance may only update pricing
                  for existing operator-added products.
                </p>
              </div>
            </div>

            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Product Name</th>
                    <th>Fuel Type</th>
                    <th>Unit Price (L)</th>
                    <th>Unit Price (CBM)</th>
                    <th>Density</th>
                    <th>Stock Available</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((prod) => (
                    <tr key={prod.id}>
                      <td>
                        <strong>{prod.productName}</strong>
                      </td>
                      <td>
                        <span
                          className={`fef-badge fef-badge-${prod.fuelType?.toLowerCase() === "pms" ? "accent" : "primary"}`}
                        >
                          {prod.fuelType}
                        </span>
                      </td>
                      <td style={{ fontWeight: 600 }}>${prod.unitPrice?.toFixed(2)}</td>
                      <td style={{ color: "var(--feftms-text-muted)" }}>
                        $
                        {(prod.unitPrice * 1000).toLocaleString(undefined, {
                          minimumFractionDigits: 2,
                        })}
                      </td>
                      <td>{prod.density?.toFixed(3)} kg/L</td>
                      <td>
                        <span
                          style={{
                            fontWeight: 600,
                            color:
                              prod.availableQuantity <= 1000 ? "var(--feftms-danger)" : "inherit",
                          }}
                        >
                          {prod.availableQuantity?.toLocaleString()} L
                        </span>
                      </td>
                      <td>
                        <button
                          className="fef-btn fef-btn-outline"
                          style={{ padding: "4px 8px", fontSize: "12px" }}
                          onClick={() => handleEditPriceClick(prod)}
                        >
                          <FiDollarSign style={{ marginRight: 4 }} /> Edit Price
                        </button>
                      </td>
                    </tr>
                  ))}
                  {products.length === 0 && !loading && (
                    <tr>
                      <td
                        colSpan="7"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No fuel products registered.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* INVOICES TAB */}
        {activeTab === "invoices" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Ledger Invoices Summary</h3>
            </div>
            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Invoice #</th>
                    <th>Customer Name</th>
                    <th>Fuel Product</th>
                    <th>Quantity (L)</th>
                    <th>Grand Total</th>
                    <th>Payment Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {invoices.map((inv) => {
                    const o = inv.order || {};
                    const curSymbol = o.currency === "TZS" ? "TZS" : "$";
                    return (
                      <tr key={inv.id}>
                        <td>
                          <strong>{inv.invoiceNumber}</strong>
                        </td>
                        <td>
                          {(o.customerName === "Stranded Drivers (Emergency Requests)" ||
                            o.customerName === "Customer Fuel Requests") &&
                          o.driverName
                            ? o.driverName
                            : o.customerName === "Stranded Drivers (Emergency Requests)"
                              ? "Customer Fuel Requests"
                              : o.customerName || "Customer"}
                        </td>
                        <td>{o.productName}</td>
                        <td>{o.quantity?.toLocaleString()} L</td>
                        <td style={{ color: "var(--feftms-success)", fontWeight: 600 }}>
                          {curSymbol}{" "}
                          {inv.grandTotal?.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </td>
                        <td>
                          <span
                            className={`fef-badge fef-badge-${inv.paymentStatus?.toLowerCase()}`}
                          >
                            {inv.paymentStatus === "PENDING_PAYMENT"
                              ? "Pending Payment"
                              : inv.paymentStatus === "PAID"
                                ? "Paid"
                                : inv.paymentStatus}
                          </span>
                        </td>
                        <td>
                          <button
                            className="fef-btn fef-btn-primary"
                            style={{ padding: "6px 12px", fontSize: "13px" }}
                            onClick={() => setSelectedInvoice(inv)}
                          >
                            Review
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                  {invoices.length === 0 && (
                    <tr>
                      <td
                        colSpan="7"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No invoices generated yet. Approving fuel orders will generate them
                        automatically.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* PAYMENT ACCOUNTS TAB */}
        {activeTab === "paymentAccounts" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div
              className="fef-panel-head"
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: 20,
              }}
            >
              <div>
                <h3>Finance-Managed Payment Accounts</h3>
                <p style={{ margin: 0, color: "var(--feftms-text-muted)" }}>
                  Configure the bank accounts and payment methods that will be dynamically printed
                  on invoices.
                </p>
              </div>
              <button
                className="fef-btn fef-btn-primary"
                onClick={() => handleOpenAccountModal(null)}
              >
                + Add Payment Account
              </button>
            </div>

            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Bank/Method</th>
                    <th>Beneficiary</th>
                    <th>Account Info</th>
                    <th>Currency</th>
                    <th>Payment Terms</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {paymentAccounts.map((acc) => (
                    <tr key={acc.id}>
                      <td>
                        <strong>{acc.bankName || acc.paymentMethod}</strong>
                        <span
                          style={{
                            display: "block",
                            fontSize: "11px",
                            color: "var(--feftms-text-muted)",
                          }}
                        >
                          Method: {acc.paymentMethod}
                        </span>
                      </td>
                      <td>{acc.beneficiaryName}</td>
                      <td>
                        <div>No: {acc.accountNumber}</div>
                        {acc.swiftCode && (
                          <div style={{ fontSize: "11px", color: "var(--feftms-text-muted)" }}>
                            SWIFT: {acc.swiftCode}
                          </div>
                        )}
                        {acc.branchName && (
                          <div style={{ fontSize: "11px", color: "var(--feftms-text-muted)" }}>
                            Branch: {acc.branchName}
                          </div>
                        )}
                      </td>
                      <td>
                        <span
                          className={`fef-badge fef-badge-${acc.currency === "TZS" ? "accent" : "primary"}`}
                          style={{ fontSize: "11px" }}
                        >
                          {acc.currency}
                        </span>
                      </td>
                      <td>
                        {acc.paymentTerms} (Validity: {acc.validityDays} days)
                      </td>
                      <td>
                        <span
                          className={`fef-badge fef-badge-${acc.status?.toLowerCase() === "active" ? "success" : "pending"}`}
                        >
                          {acc.status}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "8px" }}>
                          <button
                            className="fef-btn fef-btn-outline"
                            style={{ padding: "4px 8px", fontSize: "12px" }}
                            onClick={() => handleOpenAccountModal(acc)}
                          >
                            Edit
                          </button>
                          <button
                            className="fef-btn fef-btn-outline"
                            style={{ padding: "4px 8px", fontSize: "12px" }}
                            onClick={() => handleToggleAccountStatus(acc.id)}
                          >
                            {acc.status === "ACTIVE" ? "Deactivate" : "Activate"}
                          </button>
                          <button
                            className="fef-btn fef-btn-danger"
                            style={{
                              padding: "4px 8px",
                              fontSize: "12px",
                              background: "var(--feftms-danger)",
                            }}
                            onClick={() => handleDeleteAccount(acc.id)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {paymentAccounts.length === 0 && (
                    <tr>
                      <td
                        colSpan="7"
                        style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}
                      >
                        No payment accounts configured yet. Click "+ Add Payment Account" to get
                        started.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* COMPANY SETTINGS TAB */}
        {activeTab === "companySettings" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head" style={{ marginBottom: 20 }}>
              <div>
                <h3>Company Settings Management</h3>
                <p style={{ margin: 0, color: "var(--feftms-text-muted)" }}>
                  Manage global company details and authorized signatories printed on invoices.
                </p>
              </div>
            </div>
            <form onSubmit={handleSaveCompanySettings} style={{ padding: "0 20px 20px 20px" }}>
              <div style={{ display: "flex", gap: "20px", marginBottom: "15px" }}>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Company Name
                  </label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={companySettings.companyName || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, companyName: e.target.value })
                    }
                  />
                </div>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Email Address
                  </label>
                  <input
                    required
                    type="email"
                    className="fef-input"
                    value={companySettings.email || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, email: e.target.value })
                    }
                  />
                </div>
              </div>
              <div style={{ display: "flex", gap: "20px", marginBottom: "15px" }}>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Phone Number
                  </label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={companySettings.phoneNumber || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, phoneNumber: e.target.value })
                    }
                  />
                </div>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Logo Path / URL
                  </label>
                  <input
                    type="text"
                    className="fef-input"
                    value={companySettings.logo || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, logo: e.target.value })
                    }
                  />
                </div>
              </div>
              <div style={{ display: "flex", gap: "20px", marginBottom: "15px" }}>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Postal Address
                  </label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={companySettings.postalAddress || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, postalAddress: e.target.value })
                    }
                  />
                </div>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Office Address (Physical Location)
                  </label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={companySettings.officeAddress || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, officeAddress: e.target.value })
                    }
                  />
                </div>
              </div>
              <div
                style={{
                  borderTop: "1px dashed rgba(255,255,255,0.1)",
                  margin: "20px 0",
                  paddingTop: "20px",
                }}
              >
                <h4 style={{ color: "white", marginBottom: "15px" }}>
                  Signatory & Authorized Verification Settings
                </h4>
              </div>
              <div style={{ display: "flex", gap: "20px", marginBottom: "15px" }}>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Authorized Signatory Name
                  </label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={companySettings.signatoryName || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, signatoryName: e.target.value })
                    }
                  />
                </div>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Authorized Signatory Title
                  </label>
                  <input
                    required
                    type="text"
                    className="fef-input"
                    value={companySettings.signatoryTitle || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, signatoryTitle: e.target.value })
                    }
                  />
                </div>
              </div>
              <div style={{ display: "flex", gap: "20px", marginBottom: "20px" }}>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Authorized Signature Path / URL
                  </label>
                  <input
                    type="text"
                    className="fef-input"
                    value={companySettings.signatorySignature || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, signatorySignature: e.target.value })
                    }
                  />
                </div>
                <div className="fef-field" style={{ flex: 1 }}>
                  <label className="fef-label" style={{ color: "var(--feftms-text-muted)" }}>
                    Company Stamp Path / URL
                  </label>
                  <input
                    type="text"
                    className="fef-input"
                    value={companySettings.stamp || ""}
                    onChange={(e) =>
                      setCompanySettings({ ...companySettings, stamp: e.target.value })
                    }
                  />
                </div>
              </div>
              <button type="submit" className="fef-btn fef-btn-primary" disabled={savingSettings}>
                {savingSettings ? "Saving Settings..." : "Save Settings"}
              </button>
            </form>
          </div>
        )}

        {/* MODAL: EDIT PRICE */}
        {showEditModal &&
          selectedProduct &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setShowEditModal(false)}>
              <div
                className="fef-modal-window"
                style={{ maxWidth: "450px" }}
                onClick={(e) => e.stopPropagation()}
              >
                <button className="fef-modal-close" onClick={() => setShowEditModal(false)}>
                  <FiX />
                </button>
                <div className="fef-detail-modal-header">
                  <h2 className="fef-detail-modal-title">Edit Price</h2>
                  <p style={{ margin: "4px 0 0", color: "var(--feftms-text-muted)" }}>
                    Update selling price for {selectedProduct.productName}
                  </p>
                </div>
                <form onSubmit={handleSavePrice} style={{ marginTop: 20 }}>
                  <div className="fef-field" style={{ marginBottom: 20 }}>
                    <label className="fef-label">New Price per Litre ($)</label>
                    <input
                      required
                      type="number"
                      step="0.01"
                      min="0.01"
                      className="fef-input"
                      value={newPrice}
                      onChange={(e) => setNewPrice(e.target.value)}
                      placeholder="e.g. 1.85"
                    />
                  </div>
                  <div className="fef-detail-actions" style={{ marginTop: 24 }}>
                    <button
                      type="button"
                      className="fef-btn fef-btn-outline"
                      onClick={() => setShowEditModal(false)}
                    >
                      Cancel
                    </button>
                    <button type="submit" className="fef-btn fef-btn-primary">
                      <FiCheck style={{ marginRight: 4 }} /> Update Price
                    </button>
                  </div>
                </form>
              </div>
            </div>,
            document.body,
          )}

        {/* MODAL: ADD/EDIT PAYMENT ACCOUNT */}
        {showAccountModal &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setShowAccountModal(false)}>
              <div
                className="fef-modal-window"
                style={{ maxWidth: "550px", color: "#1e293b" }}
                onClick={(e) => e.stopPropagation()}
              >
                <button className="fef-modal-close" onClick={() => setShowAccountModal(false)}>
                  <FiX />
                </button>
                <div className="fef-detail-modal-header">
                  <h2 className="fef-detail-modal-title">
                    {editingAccount ? "Edit Payment Account" : "Add Payment Account"}
                  </h2>
                  <p style={{ margin: "4px 0 0", color: "var(--feftms-text-muted)" }}>
                    Configure payment instructions and bank details for invoices.
                  </p>
                </div>
                <form
                  onSubmit={handleSaveAccount}
                  style={{ marginTop: 20, display: "flex", flexDirection: "column", gap: "15px" }}
                >
                  <div style={{ display: "flex", gap: "15px" }}>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Payment Method</label>
                      <select
                        className="fef-input"
                        value={accountForm.paymentMethod}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, paymentMethod: e.target.value })
                        }
                      >
                        <option value="Bank Transfer">Bank Transfer</option>
                        <option value="Mobile Money">Mobile Money</option>
                        <option value="Cash">Cash</option>
                        <option value="Other">Other</option>
                      </select>
                    </div>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Currency</label>
                      <select
                        className="fef-input"
                        value={accountForm.currency}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, currency: e.target.value })
                        }
                      >
                        <option value="USD">USD</option>
                        <option value="TZS">TZS</option>
                      </select>
                    </div>
                  </div>

                  <div className="fef-field">
                    <label className="fef-label">Beneficiary Name</label>
                    <input
                      required
                      type="text"
                      className="fef-input"
                      value={accountForm.beneficiaryName}
                      onChange={(e) =>
                        setAccountForm({ ...accountForm, beneficiaryName: e.target.value })
                      }
                      placeholder="e.g. FALCON ENERGY LIMITED"
                    />
                  </div>

                  <div style={{ display: "flex", gap: "15px" }}>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Bank Name</label>
                      <input
                        required
                        type="text"
                        className="fef-input"
                        value={accountForm.bankName}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, bankName: e.target.value })
                        }
                        placeholder="e.g. CRDB BANK"
                      />
                    </div>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Branch Name</label>
                      <input
                        type="text"
                        className="fef-input"
                        value={accountForm.branchName}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, branchName: e.target.value })
                        }
                        placeholder="e.g. TEMEKE TAIFA BRANCH"
                      />
                    </div>
                  </div>

                  <div style={{ display: "flex", gap: "15px" }}>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Account Number</label>
                      <input
                        required
                        type="text"
                        className="fef-input"
                        value={accountForm.accountNumber}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, accountNumber: e.target.value })
                        }
                        placeholder="e.g. 025000130UJ00"
                      />
                    </div>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">SWIFT Code</label>
                      <input
                        type="text"
                        className="fef-input"
                        value={accountForm.swiftCode}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, swiftCode: e.target.value })
                        }
                        placeholder="e.g. CORUTZTZ"
                      />
                    </div>
                  </div>

                  <div style={{ display: "flex", gap: "15px" }}>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Payment Terms</label>
                      <input
                        type="text"
                        className="fef-input"
                        value={accountForm.paymentTerms}
                        onChange={(e) =>
                          setAccountForm({ ...accountForm, paymentTerms: e.target.value })
                        }
                        placeholder="e.g. PREPAYMENT"
                      />
                    </div>
                    <div className="fef-field" style={{ flex: 1 }}>
                      <label className="fef-label">Validity (Days)</label>
                      <input
                        required
                        type="number"
                        min="1"
                        className="fef-input"
                        value={accountForm.validityDays}
                        onChange={(e) =>
                          setAccountForm({
                            ...accountForm,
                            validityDays: parseInt(e.target.value) || 30,
                          })
                        }
                      />
                    </div>
                  </div>

                  <div className="fef-field">
                    <label className="fef-label">Payment Instructions / Notes</label>
                    <textarea
                      className="fef-textarea"
                      value={accountForm.paymentInstructions}
                      onChange={(e) =>
                        setAccountForm({ ...accountForm, paymentInstructions: e.target.value })
                      }
                      placeholder="e.g. Please pay the above amount by TT to..."
                      style={{ minHeight: "80px" }}
                    />
                  </div>

                  <div className="fef-detail-actions" style={{ marginTop: 15 }}>
                    <button
                      type="button"
                      className="fef-btn fef-btn-outline"
                      onClick={() => setShowAccountModal(false)}
                    >
                      Cancel
                    </button>
                    <button type="submit" className="fef-btn fef-btn-primary">
                      <FiCheck style={{ marginRight: 4 }} />{" "}
                      {editingAccount ? "Save Changes" : "Create Account"}
                    </button>
                  </div>
                </form>
              </div>
            </div>,
            document.body,
          )}

        {selectedInvoice && (
          <InvoiceModal
            invoice={selectedInvoice}
            onClose={() => setSelectedInvoice(null)}
            onRefresh={loadData}
            userRole="FINANCE"
          />
        )}
      </DashboardLayout>
    </RouteGuard>
  );
}
