import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import {
  FiHome,
  FiFileText,
  FiDollarSign,
  FiEdit,
  FiCheck,
  FiX,
  FiAlertCircle,
  FiCheckCircle,
  FiDroplet,
  FiActivity,
  FiCreditCard
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { listProducts, updateProduct } from "../services/productService";
import { listOrders } from "../services/orderService";
import { listInvoices } from "../services/invoiceService";
import { InvoiceModal } from "../components/InvoiceModal";
import "../styles/forms.css";

export const Route = createFileRoute("/finance")({
  head: () => ({ meta: [{ title: "Finance Desk — FEFTMS" }] }),
  component: FinanceDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "pricing", label: "Fuel Pricing", icon: FiDollarSign },
  { key: "invoices", label: "Invoices", icon: FiFileText },
];

function FinanceDash() {
  const [activeTab, setActiveTab] = useState("dash");
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modals
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [newPrice, setNewPrice] = useState("");

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
        density: selectedProduct.density,
        unitPrice: price,
      });

      setSuccess(`Price for ${selectedProduct.productName} updated successfully to $${price}/L.`);
      setShowEditModal(false);
      loadProducts();
    } catch (err) {
      setError(err?.message || "Failed to update price.");
    }
  };

  const totalProducts = products.length;
  const avgPrice = totalProducts > 0 
    ? (products.reduce((acc, p) => acc + p.unitPrice, 0) / totalProducts).toFixed(2) 
    : "0.00";
  const totalStock = products.reduce((acc, p) => acc + (p.availableQuantity || 0), 0);

  const approvedOrders = orders.filter(o => o.orderStatus === "APPROVED" || o.orderStatus === "DELIVERED");
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
          title={activeTab === "dash" ? "Finance Console" : activeTab === "pricing" ? "Fuel Price Control" : activeTab.charAt(0).toUpperCase() + activeTab.slice(1)} 
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
          <StatCard label="Average Price/L" value={`$${avgPrice}`} icon={FiDollarSign} tone="accent" />
          <StatCard label="System Stock Volume" value={`${totalStock.toLocaleString()} L`} icon={FiActivity} tone="success" />
          <StatCard label="Revenue (30d)" value={`$${recentRevenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`} icon={FiCreditCard} tone="primary" />
          <StatCard label="Sales (30d)" value={recentOrdersCount} icon={FiFileText} tone="accent" />
        </div>

        {/* DASHBOARD TAB */}
        {activeTab === "dash" && (
          <div className="fef-panel" style={{ marginTop: 24 }}>
            <div className="fef-panel-head">
              <h3>Recent Financial Ledger</h3>
            </div>
            <div style={{ padding: 20, color: "var(--feftms-text-muted)" }}>
              Finance users manage fuel pricing in real-time. Use the **Fuel Pricing** tab to control pricing variables and synchronize changes across all dashboards.
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
                  Fuel products are created by the operations team. Finance may only update pricing for existing operator-added products.
                </p>
              </div>
            </div>

            <div className="fef-table-wrap">
              <table className="fef-table">
                <thead>
                  <tr>
                    <th>Product Name</th>
                    <th>Fuel Type</th>
                    <th>Density (kg/L)</th>
                    <th>Current Selling Price</th>
                    <th>Available Quantity (Read-only)</th>
                    <th>Availability Status (Read-only)</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((p) => (
                    <tr key={p.id}>
                      <td><strong>{p.productName}</strong></td>
                      <td>{p.fuelType}</td>
                      <td>{p.density}</td>
                      <td style={{ color: "var(--feftms-primary)", fontWeight: 700 }}>
                        ${p.unitPrice?.toFixed(2)}/L
                      </td>
                      <td>{p.availableQuantity?.toLocaleString()} Liters</td>
                      <td>
                        <span className={`fef-badge fef-badge-${p.status?.toLowerCase() === 'active' || p.status?.toLowerCase() === 'available' ? 'success' : 'danger'}`}>
                          {p.status}
                        </span>
                      </td>
                      <td>
                        <button 
                          className="fef-btn fef-btn-outline" 
                          style={{ padding: "6px 12px", fontSize: "13px" }}
                          onClick={() => handleEditPriceClick(p)}
                        >
                          <FiEdit style={{ marginRight: 4 }} /> Edit Price
                        </button>
                      </td>
                    </tr>
                  ))}
                  {products.length === 0 && !loading && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
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
                    return (
                      <tr key={inv.id}>
                        <td><strong>{inv.invoiceNumber}</strong></td>
                        <td>{o.customerName === "Stranded Drivers (Emergency Requests)" && o.driverName ? o.driverName : (o.customerName || "Stranded Driver")}</td>
                        <td>{o.productName}</td>
                        <td>{o.quantity?.toLocaleString()} L</td>
                        <td style={{ color: "var(--feftms-success)", fontWeight: 600 }}>
                          ${inv.grandTotal?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </td>
                        <td>
                          <span className={`fef-badge fef-badge-${inv.paymentStatus?.toLowerCase()}`}>
                            {inv.paymentStatus === "PENDING_PAYMENT" ? "Pending Payment" : inv.paymentStatus === "PAID" ? "Paid" : inv.paymentStatus}
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
                      <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)" }}>
                        No invoices generated yet. Approving fuel orders will generate them automatically.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* MODAL: EDIT PRICE */}
        {showEditModal && selectedProduct && typeof window !== "undefined" && createPortal(
          <div className="fef-modal-backdrop" onClick={() => setShowEditModal(false)}>
            <div className="fef-modal-window" style={{ maxWidth: "450px" }} onClick={(e) => e.stopPropagation()}>
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
                  <button type="button" className="fef-btn fef-btn-outline" onClick={() => setShowEditModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="fef-btn fef-btn-primary">
                    <FiCheck style={{ marginRight: 4 }} /> Update Price
                  </button>
                </div>
              </form>
            </div>
          </div>,
          document.body
        )}

        {selectedInvoice && (
          <InvoiceModal
            invoice={selectedInvoice}
            onClose={() => setSelectedInvoice(null)}
            onRefresh={() => {
              loadInvoices();
              const endDate = new Date().toISOString();
              const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();
              listOrders({ startDate, endDate, size: 200 })
                .then((res) => {
                  setOrders(res.content || []);
                });
            }}
            userRole="FINANCE"
          />
        )}

      </DashboardLayout>
    </RouteGuard>
  );
}
