import { useState } from "react";
import { FiX, FiPrinter, FiCheck, FiAlertCircle, FiCheckCircle } from "react-icons/fi";
import { approveInvoicePayment } from "../services/invoiceService";

export function InvoiceModal({ invoice, onClose, onRefresh, userRole }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  if (!invoice) return null;

  const handleApprovePayment = async () => {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      await approveInvoicePayment(invoice.id);
      setSuccess("Invoice payment processed and marked as PAID successfully.");
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to process payment approval.");
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    const printContent = document.getElementById("invoice-printable-area").innerHTML;
    const originalContent = document.body.innerHTML;
    
    // Create print stylesheet dynamically
    const style = document.createElement('style');
    style.innerHTML = `
      @media print {
        body {
          background: white !important;
          color: black !important;
          padding: 20px !important;
        }
        .no-print {
          display: none !important;
        }
        .invoice-card {
          border: none !important;
          box-shadow: none !important;
          background: white !important;
          color: black !important;
          width: 100% !important;
          max-width: 100% !important;
          margin: 0 !important;
          padding: 0 !important;
        }
        table {
          width: 100% !important;
          border-collapse: collapse !important;
        }
        th, td {
          border-bottom: 1px solid #ddd !important;
          padding: 8px !important;
        }
      }
    `;
    document.head.appendChild(style);
    window.print();
    document.head.removeChild(style);
  };

  const order = invoice.order || {};
  const customer = order.customer || {};
  const product = order.product || {};

  const isPending = invoice.paymentStatus?.toUpperCase() === "PENDING";
  const isPaid = invoice.paymentStatus?.toUpperCase() === "PAID";

  return (
    <div className="fef-modal-backdrop" onClick={onClose} style={{ zIndex: 9999 }}>
      <div 
        className="fef-modal-window" 
        style={{ maxWidth: "750px", width: "100%", maxHeight: "90vh", display: "flex", flexDirection: "column", padding: 0, overflow: "hidden" }} 
        onClick={(e) => e.stopPropagation()}
      >
        {/* Sticky Modal Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "16px 24px", borderBottom: "1px solid rgba(255,255,255,0.1)", background: "rgba(30, 41, 59, 0.95)" }}>
          <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "600", display: "flex", alignItems: "center", gap: "8px" }}>
            <span>Invoice Details</span>
            <span className={`fef-badge fef-badge-${invoice.paymentStatus?.toLowerCase()}`} style={{ fontSize: "12px", padding: "2px 8px" }}>
              {invoice.paymentStatus}
            </span>
          </h3>
          <div style={{ display: "flex", gap: "8px" }}>
            <button className="fef-btn fef-btn-outline no-print" onClick={handlePrint} style={{ padding: "6px 12px", display: "flex", alignItems: "center", gap: "6px", fontSize: "13px" }}>
              <FiPrinter /> Print
            </button>
            <button className="fef-modal-close" onClick={onClose} style={{ position: "static", background: "none", border: "none", color: "var(--feftms-text-muted)", cursor: "pointer" }}>
              <FiX size={20} />
            </button>
          </div>
        </div>

        {/* Modal Scrollable Content */}
        <div style={{ overflowY: "auto", padding: "24px", flex: 1, background: "#f8fafc", color: "#1e293b" }}>
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

          <div id="invoice-printable-area">
            <div className="invoice-card" style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: "8px", padding: "32px", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)" }}>
              
              {/* Header: Company Logo & Info */}
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", borderBottom: "2px solid #e2e8f0", paddingBottom: "24px", marginBottom: "24px" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
                    <div style={{ background: "linear-gradient(135deg, #f97316 0%, #ea580c 100%)", width: "32px", height: "32px", borderRadius: "8px", display: "flex", alignItems: "center", justifyContent: "center", color: "white", fontWeight: "bold" }}>
                      F
                    </div>
                    <span style={{ fontSize: "20px", fontWeight: "800", color: "#0f172a", letterSpacing: "0.5px" }}>FALCON ENERGY LTD</span>
                  </div>
                  <div style={{ fontSize: "12px", color: "#64748b", lineHeight: "1.6" }}>
                    Falcon Tower, Mombasa Road<br />
                    P.O. Box 99882 - 00100, Nairobi, Kenya<br />
                    Tel: +254 700 000 000 | Email: billing@falconenergy.com<br />
                    VAT No: VAT-FE-998822 | TIN: P051234567Z
                  </div>
                </div>
                <div style={{ textAlign: "right" }}>
                  <h1 style={{ margin: "0 0 6px 0", fontSize: "28px", fontWeight: "900", color: "#0f172a", letterSpacing: "-0.5px" }}>INVOICE</h1>
                  <table style={{ borderCollapse: "collapse", fontSize: "12px", color: "#64748b", float: "right" }}>
                    <tbody>
                      <tr>
                        <td style={{ padding: "2px 8px", fontWeight: "bold", color: "#334155", textAlign: "right" }}>Invoice No:</td>
                        <td style={{ padding: "2px 8px", color: "#0f172a", fontWeight: "bold" }}>{invoice.invoiceNumber}</td>
                      </tr>
                      <tr>
                        <td style={{ padding: "2px 8px", fontWeight: "bold", color: "#334155", textAlign: "right" }}>Date:</td>
                        <td style={{ padding: "2px 8px", color: "#0f172a" }}>
                          {invoice.invoiceDate ? new Date(invoice.invoiceDate).toLocaleDateString("en-KE", { day: "numeric", month: "short", year: "numeric" }) : ""}
                        </td>
                      </tr>
                      <tr>
                        <td style={{ padding: "2px 8px", fontWeight: "bold", color: "#334155", textAlign: "right" }}>Order Ref:</td>
                        <td style={{ padding: "2px 8px", color: "#0f172a" }}>{order.orderNumber}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Bill To & Details Grid */}
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px", marginBottom: "28px" }}>
                <div>
                  <h4 style={{ margin: "0 0 8px 0", fontSize: "12px", textTransform: "uppercase", color: "#64748b", letterSpacing: "0.5px" }}>BILLED TO:</h4>
                  <div style={{ fontSize: "14px", fontWeight: "bold", color: "#0f172a", marginBottom: "4px" }}>{order.customerName}</div>
                  <div style={{ fontSize: "12px", color: "#475569", lineHeight: "1.5" }}>
                    {customer.contactPerson && <div>Attn: {customer.contactPerson}</div>}
                    {customer.address && <div>Address: {customer.address}</div>}
                    {customer.phone && <div>Tel: {customer.phone}</div>}
                    {customer.email && <div>Email: {customer.email}</div>}
                    {customer.tinNumber && <div style={{ marginTop: "4px", fontSize: "11px", color: "#64748b" }}>TIN/KRA PIN: <strong>{customer.tinNumber}</strong></div>}
                  </div>
                </div>

                <div style={{ background: "#f1f5f9", borderRadius: "6px", padding: "16px", fontSize: "12px", color: "#475569" }}>
                  <h4 style={{ margin: "0 0 8px 0", fontSize: "11px", textTransform: "uppercase", color: "#64748b", letterSpacing: "0.5px" }}>DELIVERY DETAILS:</h4>
                  <div style={{ lineHeight: "1.6" }}>
                    {order.locationAddress && <div>📍 <strong>Address:</strong> {order.locationAddress}</div>}
                    {order.driverName && <div>🚛 <strong>Driver:</strong> {order.driverName} ({order.driverPhone})</div>}
                    {order.vehicleType && <div>🚒 <strong>Vehicle Type:</strong> <span style={{ textTransform: "capitalize" }}>{order.vehicleType}</span></div>}
                    {order.paymentMethod && <div>💳 <strong>Payment Method:</strong> {order.paymentMethod}</div>}
                  </div>
                </div>
              </div>

              {/* Items Table */}
              <table style={{ width: "100%", borderCollapse: "collapse", marginBottom: "24px" }}>
                <thead>
                  <tr style={{ background: "#0f172a", color: "white" }}>
                    <th style={{ padding: "10px 12px", textAlign: "left", fontSize: "12px", textTransform: "uppercase" }}>Description (Fuel Product)</th>
                    <th style={{ padding: "10px 12px", textAlign: "right", fontSize: "12px", textTransform: "uppercase" }}>Approved Qty</th>
                    <th style={{ padding: "10px 12px", textAlign: "right", fontSize: "12px", textTransform: "uppercase" }}>Unit Price</th>
                    <th style={{ padding: "10px 12px", textAlign: "right", fontSize: "12px", textTransform: "uppercase" }}>Total Amount</th>
                  </tr>
                </thead>
                <tbody>
                  <tr style={{ borderBottom: "1px solid #e2e8f0" }}>
                    <td style={{ padding: "12px", fontSize: "13px", color: "#0f172a" }}>
                      <strong>{order.productName || product.productName}</strong>
                      <div style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>Fuel Type: {product.fuelType || "N/A"} | Density: {product.density || "N/A"} kg/L</div>
                    </td>
                    <td style={{ padding: "12px", textAlign: "right", fontSize: "13px", color: "#0f172a" }}>
                      {order.quantity?.toLocaleString()} L
                    </td>
                    <td style={{ padding: "12px", textAlign: "right", fontSize: "13px", color: "#0f172a" }}>
                      ${product.unitPrice?.toFixed(2)}/L
                    </td>
                    <td style={{ padding: "12px", textAlign: "right", fontSize: "13px", color: "#0f172a", fontWeight: "600" }}>
                      ${invoice.subtotal?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </td>
                  </tr>
                </tbody>
              </table>

              {/* History / Modification Log */}
              {order.originalQuantity && order.originalQuantity !== order.quantity && (
                <div style={{ background: "#fffbeb", border: "1px solid #fef3c7", borderRadius: "6px", padding: "12px", marginBottom: "24px", fontSize: "12px", color: "#92400e" }}>
                  <strong>⚠️ Order Adjustment Audit Trail:</strong>
                  <div style={{ marginTop: "4px", lineHeight: "1.4" }}>
                    • Original Customer Requested Quantity: <strong>{order.originalQuantity?.toLocaleString()} L</strong><br />
                    • Approved / Adjusted Quantity: <strong>{order.quantity?.toLocaleString()} L</strong><br />
                    {order.editReason && <span>• Reason for Adjustment: <em>"{order.editReason}"</em></span>}
                  </div>
                </div>
              )}

              {/* Financial Calculation Summary */}
              <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: "32px" }}>
                <table style={{ width: "280px", borderCollapse: "collapse", fontSize: "13px" }}>
                  <tbody>
                    <tr style={{ borderBottom: "1px solid #e2e8f0" }}>
                      <td style={{ padding: "8px 0", color: "#64748b" }}>Subtotal:</td>
                      <td style={{ padding: "8px 0", textAlign: "right", color: "#0f172a", fontWeight: "600" }}>
                        ${invoice.subtotal?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </td>
                    </tr>
                    <tr style={{ borderBottom: "1px solid #e2e8f0" }}>
                      <td style={{ padding: "8px 0", color: "#64748b" }}>VAT (16%):</td>
                      <td style={{ padding: "8px 0", textAlign: "right", color: "#0f172a", fontWeight: "600" }}>
                        ${invoice.tax?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </td>
                    </tr>
                    <tr style={{ borderBottom: "2px solid #0f172a" }}>
                      <td style={{ padding: "10px 0", color: "#0f172a", fontWeight: "800", fontSize: "15px" }}>Grand Total:</td>
                      <td style={{ padding: "10px 0", textAlign: "right", color: "var(--feftms-primary, #ea580c)", fontWeight: "800", fontSize: "16px" }}>
                        ${invoice.grandTotal?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {/* Terms and Finance Approval Grid */}
              <div style={{ display: "grid", gridTemplateColumns: "1.2fr 0.8fr", gap: "24px", fontSize: "11px", color: "#64748b", borderTop: "1px solid #e2e8f0", paddingTop: "20px" }}>
                <div>
                  <h4 style={{ margin: "0 0 6px 0", fontSize: "11px", color: "#334155", textTransform: "uppercase" }}>Terms & Conditions</h4>
                  <div style={{ whiteSpace: "pre-line", lineHeight: "1.5" }}>
                    {invoice.termsAndConditions}
                  </div>
                </div>

                <div style={{ display: "flex", flexDirection: "column", justifyContent: "flex-end", alignItems: "flex-end", textAlign: "right" }}>
                  <h4 style={{ margin: "0 0 6px 0", fontSize: "11px", color: "#334155", textTransform: "uppercase" }}>Finance Approval</h4>
                  {isPaid ? (
                    <div style={{ border: "2px solid #16a34a", color: "#16a34a", padding: "8px 12px", borderRadius: "6px", background: "#f0fdf4", display: "inline-block", fontWeight: "bold", textTransform: "uppercase", fontSize: "12px", textAlign: "center" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: "4px", justifyContent: "center" }}>
                        <FiCheckCircle /> PAID & APPROVED
                      </div>
                      <div style={{ fontSize: "9px", color: "#15803d", fontWeight: "normal", textTransform: "none", marginTop: "2px" }}>
                        By: {invoice.financeApprovedBy}<br />
                        At: {new Date(invoice.financeApprovedAt).toLocaleString()}
                      </div>
                    </div>
                  ) : (
                    <div style={{ border: "2px dashed #ca8a04", color: "#ca8a04", padding: "8px 12px", borderRadius: "6px", background: "#fef9c3", display: "inline-block", fontWeight: "bold", textTransform: "uppercase", fontSize: "11px" }}>
                      Payment Pending
                    </div>
                  )}
                </div>
              </div>

            </div>
          </div>
        </div>

        {/* Sticky Modal Footer: Action Bar */}
        {isPending && userRole === "FINANCE" && (
          <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", padding: "16px 24px", borderTop: "1px solid rgba(255,255,255,0.1)", background: "rgba(30, 41, 59, 0.95)" }}>
            <button className="fef-btn fef-btn-outline" onClick={onClose} disabled={loading}>
              Close
            </button>
            <button className="fef-btn fef-btn-success" onClick={handleApprovePayment} disabled={loading} style={{ display: "flex", alignItems: "center", gap: "6px" }}>
              <FiCheck /> {loading ? "Processing..." : "Approve Payment"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
