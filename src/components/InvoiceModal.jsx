import { useState } from "react";
import { createPortal } from "react-dom";
import { FiX, FiPrinter, FiCheck, FiAlertCircle, FiCheckCircle } from "react-icons/fi";
import { approveInvoicePayment, overrideInvoiceStatus } from "../services/invoiceService";
import logoImg from "../assets/falcon-logo.png";
import signatureImg from "../assets/authorized-signature.png";
import stampImg from "../assets/falcon-stamp.png";

// Helper to convert number to words for USD amounts
function numberToWords(num) {
  if (num === null || num === undefined || isNaN(num)) return "";
  
  const ones = ["", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", 
                "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"];
  const tens = ["", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"];
  const scales = ["", "THOUSAND", "MILLION", "BILLION"];

  function convertInteger(n) {
    if (n === 0) return "ZERO";
    
    let words = [];
    let scaleIndex = 0;
    
    while (n > 0) {
      let chunk = n % 1000;
      if (chunk > 0) {
        let chunkWords = [];
        let hundreds = Math.floor(chunk / 100);
        let remainder = chunk % 100;
        
        if (hundreds > 0) {
          chunkWords.push(ones[hundreds] + " HUNDRED");
        }
        
        if (remainder > 0) {
          if (remainder < 20) {
            chunkWords.push(ones[remainder]);
          } else {
            let tenPart = Math.floor(remainder / 10);
            let onePart = remainder % 10;
            chunkWords.push(tens[tenPart] + (onePart > 0 ? " " + ones[onePart] : ""));
          }
        }
        
        if (scales[scaleIndex]) {
          chunkWords.push(scales[scaleIndex]);
        }
        
        words.unshift(chunkWords.join(" "));
      }
      n = Math.floor(n / 1000);
      scaleIndex++;
    }
    
    return words.join(" ");
  }

  const parts = Number(num).toFixed(2).split(".");
  const dollars = parseInt(parts[0], 10);
  const cents = parseInt(parts[1], 10);

  let result = "UNITED STATES DOLLARS " + convertInteger(dollars);
  if (cents > 0) {
    result += " AND " + convertInteger(cents) + " CENTS";
  }
  result += " ONLY";
  
  return result.toUpperCase();
}

// Helper to format date in long format (e.g. October 21, 2025)
function formatDateLong(dateStr) {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return date.toLocaleDateString("en-US", { day: "numeric", month: "long", year: "numeric" });
}

// Helper to get ordinal suffix (1st, 2nd, etc.)
function getOrdinalSuffix(day) {
  if (day > 3 && day < 21) return 'th';
  switch (day % 10) {
    case 1:  return "st";
    case 2:  return "nd";
    case 3:  return "rd";
    default: return "th";
  }
}

// Helper to format validity date: e.g., 30th Sep, 2025
function formatValidityDate(dateObj) {
  if (!dateObj) return "";
  const day = dateObj.getDate();
  const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  const month = monthNames[dateObj.getMonth()];
  const year = dateObj.getFullYear();
  return `${day}${getOrdinalSuffix(day)} ${month}, ${year}`;
}

// Helper to separate a single address text into Box Office and Physical Address
function parseCustomerAddress(addressStr) {
  if (!addressStr) {
    return {
      postal: "BUYER BOX OFFICE ADDRESS",
      physical: "BUYER PHYSICAL ADDRESS"
    };
  }

  const lines = addressStr.split(/\n+/);
  if (lines.length > 1) {
    const postalIndex = lines.findIndex(l => /p\.?o\.?\s*box/i.test(l));
    if (postalIndex !== -1) {
      const postal = lines[postalIndex].trim();
      const physical = lines.filter((_, i) => i !== postalIndex).join(", ").trim();
      return { postal, physical: physical || "N/A" };
    }
    return {
      postal: lines[0].trim(),
      physical: lines.slice(1).join(", ").trim()
    };
  }

  const parts = addressStr.split(/,\s*/);
  const postalIndex = parts.findIndex(p => /p\.?o\.?\s*box/i.test(p));
  if (postalIndex !== -1) {
    const postal = parts[postalIndex].trim();
    const physical = parts.filter((_, i) => i !== postalIndex).join(", ").trim();
    return { postal, physical: physical || "N/A" };
  }

  if (/p\.?o\.?\s*box/i.test(addressStr)) {
    return { postal: addressStr.trim(), physical: "N/A" };
  }

  return {
    postal: "P.O. Box Not Provided",
    physical: addressStr.trim()
  };
}

export function InvoiceModal({ invoice, onClose, onRefresh, userRole }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [overrideStatus, setOverrideStatus] = useState(invoice?.paymentStatus || "PENDING_PAYMENT");

  if (!invoice) return null;

  const handleApprovePayment = async () => {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      await approveInvoicePayment(invoice.id);
      setSuccess("Invoice payment confirmed and marked as Paid successfully.");
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to confirm payment.");
    } finally {
      setLoading(false);
    }
  };

  const handleOverrideStatus = async () => {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      await overrideInvoiceStatus(invoice.id, overrideStatus);
      setSuccess(`Invoice status successfully overridden to ${overrideStatus === "PENDING_PAYMENT" ? "Pending Payment" : "Paid"}.`);
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to override invoice status.");
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const order = invoice.order || {};
  const customer = order.customer || {};
  const product = order.product || {};

  const isPending = invoice.paymentStatus?.toUpperCase() === "PENDING" || invoice.paymentStatus?.toUpperCase() === "PENDING_PAYMENT";
  const isPaid = invoice.paymentStatus?.toUpperCase() === "PAID";

  if (typeof window === "undefined") return null;

  return createPortal(
    <div className="fef-modal-backdrop" onClick={onClose} style={{ zIndex: 9999 }}>
      <div 
        className="fef-modal-window" 
        style={{ maxWidth: "750px", width: "100%", maxHeight: "90vh", display: "flex", flexDirection: "column", padding: 0, overflow: "hidden" }} 
        onClick={(e) => e.stopPropagation()}
      >
        {/* Sticky Modal Header */}
        <div className="no-print" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "16px 24px", borderBottom: "1px solid rgba(255,255,255,0.1)", background: "rgba(30, 41, 59, 0.95)" }}>
          <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "600", display: "flex", alignItems: "center", gap: "8px" }}>
            <span>Invoice Details</span>
            <span className={`fef-badge fef-badge-${invoice.paymentStatus?.toLowerCase()}`} style={{ fontSize: "12px", padding: "2px 8px" }}>
              {invoice.paymentStatus === "PENDING_PAYMENT" ? "Pending Payment" : invoice.paymentStatus === "PAID" ? "Paid" : invoice.paymentStatus}
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
        <div className="fef-modal-scrollable-content" style={{ overflowY: "auto", padding: "24px", flex: 1, background: "#f8fafc", color: "#1e293b" }}>
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
            {(() => {
              const productNameUpper = (order.productName || product.productName || "").toUpperCase();
              const fuelTypeUpper = (product.fuelType || "").toUpperCase();
              const isPMS = productNameUpper.includes("PMS") || productNameUpper.includes("PETROL") || fuelTypeUpper === "PMS";
              const productDescription = isPMS
                ? "Premium Motor Spirit – Automotive Gasoline 90 RON (According to East African Standards)"
                : "Automotive Gasoil – Diesel 50 ppm (According to East African Standards)";

              const parsedAddress = parseCustomerAddress(customer.address || order.locationAddress);

              const qtyInCbm = (order.quantity || 0) / 1000;
              const pricePerCbm = (product.unitPrice || 0) * 1000;
              const grandTotal = invoice.grandTotal || (qtyInCbm * pricePerCbm);
              const totalAmountWords = numberToWords(grandTotal);

              const invoiceDateVal = invoice.invoiceDate ? new Date(invoice.invoiceDate) : new Date();
              const validityDateVal = new Date(invoiceDateVal.getTime());
              validityDateVal.setDate(validityDateVal.getDate() + 30);
              const validityDateStr = formatValidityDate(validityDateVal);

              return (
                <div className="invoice-card" style={{ 
                  background: "#ffffff", 
                  color: "#000000",
                  border: "3px double #000000", 
                  borderRadius: "8px", 
                  padding: "40px", 
                  boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)",
                  fontFamily: "Arial, sans-serif"
                }}>
                  {/* Header: Logo and Company Info */}
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
                    <div>
                      <img src={logoImg} alt="Falcon Energy Logo" style={{ height: "90px", width: "auto" }} />
                    </div>
                    <div style={{ textAlign: "right", fontSize: "13px", lineHeight: "1.5", color: "#000000" }}>
                      <div style={{ fontSize: "20px", fontWeight: "bold", color: "#000000", letterSpacing: "0.5px", marginBottom: "4px" }}>
                        FALCON ENERGY LIMITED
                      </div>
                      <div style={{ fontWeight: "600" }}>P.O. Box : 45431, 6th Floor, SALAMANDER TOWER</div>
                      <div style={{ fontWeight: "600" }}>SAMORA AVENUE, DAR ES SALAAM</div>
                    </div>
                  </div>

                  {/* Document Title */}
                  <div style={{ 
                    textAlign: "center", 
                    fontSize: "20px", 
                    fontWeight: "bold", 
                    borderTop: "1px solid #000000", 
                    borderBottom: "1px solid #000000", 
                    padding: "6px 0",
                    marginBottom: "20px",
                    letterSpacing: "1px",
                    color: "#000000"
                  }}>
                    PROFORMA INVOICE
                  </div>

                  {/* Order adjustment warning if present */}
                  {order.originalQuantity && order.originalQuantity !== order.quantity && (
                    <div className="no-print" style={{ 
                      background: "#fffbeb", 
                      border: "1px solid #fef3c7", 
                      borderRadius: "6px", 
                      padding: "8px 12px", 
                      marginBottom: "15px", 
                      fontSize: "12px", 
                      color: "#92400e" 
                    }}>
                      <strong>⚠️ Order Adjustment Audit Trail:</strong>
                      <span style={{ marginLeft: "8px" }}>
                        Original: {order.originalQuantity?.toLocaleString()} L | Adjusted: {order.quantity?.toLocaleString()} L
                        {order.editReason && ` (${order.editReason})`}
                      </span>
                    </div>
                  )}

                  {/* Main Invoice Table */}
                  <table style={{ width: "100%", borderCollapse: "collapse", border: "1px solid #000000", fontSize: "13px", color: "#000000" }}>
                    <tbody>
                      {/* Row 1: Date & Invoice No */}
                      <tr>
                        <td style={{ width: "15%", border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>DATE:</td>
                        <td style={{ width: "45%", border: "1px solid #000000", padding: "8px 12px" }}>
                          {formatDateLong(invoice.invoiceDate)}
                        </td>
                        <td style={{ width: "15%", border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>INVOICE No:</td>
                        <td style={{ width: "25%", border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>
                          {invoice.invoiceNumber}
                        </td>
                      </tr>

                      {/* Row 2: Buyer Details */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold", verticalAlign: "top" }}>BUYER:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", verticalAlign: "top" }}>
                          <div style={{ fontWeight: "bold", marginBottom: "4px" }}>
                            {order.customerName || customer.companyName || "BUYER NAME"}
                          </div>
                          <div style={{ marginBottom: "2px" }}>{parsedAddress.postal}</div>
                          <div>{parsedAddress.physical}</div>
                        </td>
                      </tr>

                      {/* Row 3: Product Description */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold", verticalAlign: "top" }}>PRODUCT:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>
                          {productDescription}
                        </td>
                      </tr>

                      {/* Row 4: Quantity */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>QUANTITY:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px" }}>
                          {qtyInCbm.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 })} CBM
                        </td>
                      </tr>

                      {/* Row 5: Price */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>PRICE (FUEL):</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px" }}>
                          Us$ {pricePerCbm.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Per CBM
                        </td>
                      </tr>

                      {/* Row 6: Incoterms */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>INCOTERMS:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px" }}>
                          EX-TANGA PORT
                        </td>
                      </tr>

                      {/* Row 7: Grand Total Numeric */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px" }}></td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold", fontSize: "14px" }}>
                          US $ {grandTotal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </td>
                      </tr>

                      {/* Row 8: Grand Total Words */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold", verticalAlign: "top" }}>TOTAL AMOUNT:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>
                          ({totalAmountWords})
                        </td>
                      </tr>

                      {/* Row 9: Payment Instructions Header */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>PAYMENT INSTRUCTIONS:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>
                          PREPAYMENT
                        </td>
                      </tr>

                      {/* Row 10: Bank Title */}
                      <tr>
                        <td colSpan={4} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold", textDecoration: "underline" }}>
                          PLEASE PAY THE ABOVE AMOUNT BY TT TO:
                        </td>
                      </tr>

                      {/* Row 11: Bank Table & Stamp Split */}
                      <tr>
                        <td colSpan={2} style={{ width: "60%", border: "1px solid #000000", padding: 0, verticalAlign: "top" }}>
                          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "12px" }}>
                            <tbody>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td style={{ width: "30%", padding: "6px 8px", borderRight: "1px solid #000000", fontWeight: "bold" }}>Beneficiary:</td>
                                <td style={{ width: "70%", padding: "6px 8px", fontWeight: "bold" }}>FALCON ENERGY LIMITED</td>
                              </tr>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td style={{ padding: "6px 8px", borderRight: "1px solid #000000", fontWeight: "bold" }}>Bank Name:</td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>CRDB BANK</td>
                              </tr>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td style={{ padding: "6px 8px", borderRight: "1px solid #000000", fontWeight: "bold" }}>Branch Name:</td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>TEMEKE TAIFA BRANCH</td>
                              </tr>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td style={{ padding: "6px 8px", borderRight: "1px solid #000000", fontWeight: "bold" }}>Account No:</td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>025000130UJ00</td>
                              </tr>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td style={{ padding: "6px 8px", borderRight: "1px solid #000000", fontWeight: "bold" }}>Swift Code:</td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>CORUTZTZ</td>
                              </tr>
                              <tr>
                                <td style={{ padding: "6px 8px", borderRight: "1px solid #000000", fontWeight: "bold" }}>Currency:</td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>United States Dollars</td>
                              </tr>
                            </tbody>
                          </table>
                        </td>
                        <td colSpan={2} style={{ width: "40%", border: "1px solid #000000", padding: "10px", textAlign: "center", verticalAlign: "middle" }}>
                          <img src={stampImg} alt="Falcon Energy Limited Stamp" style={{ height: "115px", width: "auto", display: "inline-block" }} />
                        </td>
                      </tr>

                      {/* Row 12: Validity */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>PLEASE NOTE:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>
                          This Proforma is Valid Until {validityDateStr}
                        </td>
                      </tr>

                      {/* Row 13: FOR */}
                      <tr>
                        <td style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>FOR:</td>
                        <td colSpan={3} style={{ border: "1px solid #000000", padding: "8px 12px", fontWeight: "bold" }}>
                          FALCON ENERGY LIMITED
                        </td>
                      </tr>

                      {/* Row 14: Signature */}
                      <tr>
                        <td colSpan={4} style={{ border: "1px solid #000000", padding: "6px 12px" }}>
                          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%" }}>
                            <div style={{ fontWeight: "bold", textTransform: "uppercase", fontSize: "12px", width: "50%", textAlign: "center" }}>
                              AUTHORIZED SIGNATORY
                            </div>
                            <div style={{ width: "50%", textAlign: "right", paddingRight: "40px" }}>
                              <img src={signatureImg} alt="Authorized Signature" style={{ height: "45px", width: "auto", display: "inline-block" }} />
                            </div>
                          </div>
                        </td>
                      </tr>

                    </tbody>
                  </table>
                </div>
              );
            })()}
          </div>
        </div>
        {/* Sticky Modal Footer: Action Bar */}
        <div className="no-print" style={{ display: "flex", justifyContent: "flex-end", alignItems: "center", gap: "12px", padding: "16px 24px", borderTop: "1px solid rgba(255,255,255,0.1)", background: "rgba(30, 41, 59, 0.95)" }}>
          <button className="fef-btn fef-btn-outline" onClick={onClose} disabled={loading}>
            Close
          </button>

          {isPending && userRole === "FINANCE" && (
            <button className="fef-btn fef-btn-success" onClick={handleApprovePayment} disabled={loading} style={{ display: "flex", alignItems: "center", gap: "6px" }}>
              <FiCheck /> {loading ? "Processing..." : "Confirm Payment"}
            </button>
          )}

          {userRole === "ADMIN" && (
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <select
                value={overrideStatus}
                onChange={(e) => setOverrideStatus(e.target.value)}
                style={{
                  padding: "8px 12px",
                  borderRadius: "6px",
                  background: "#0f172a",
                  color: "white",
                  border: "1px solid rgba(255,255,255,0.2)",
                  fontSize: "13px"
                }}
                disabled={loading}
              >
                <option value="PENDING_PAYMENT">Pending Payment</option>
                <option value="PAID">Paid</option>
              </select>
              <button
                className="fef-btn fef-btn-primary"
                onClick={handleOverrideStatus}
                disabled={loading}
                style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px" }}
              >
                <FiCheck /> {loading ? "Overriding..." : "Override Status"}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
