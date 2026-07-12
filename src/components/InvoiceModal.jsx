import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { FiX, FiPrinter, FiCheck, FiAlertCircle, FiCheckCircle, FiDownload } from "react-icons/fi";
import {
  approveInvoicePayment,
  overrideInvoiceStatus,
  updateInvoicePaymentAccount,
  getInvoiceById,
} from "../services/invoiceService";
import { listActivePaymentAccounts } from "../services/paymentAccountService";
import logoImg from "../assets/falcon-logo.png";
import signatureImg from "../assets/authorized-signature.png";
import stampImg from "../assets/falcon-stamp.png";

// Helper to convert number to words for USD and TZS amounts
function numberToWords(num, currency = "USD") {
  if (num === null || num === undefined || isNaN(num)) return "";

  const ones = [
    "",
    "ONE",
    "TWO",
    "THREE",
    "FOUR",
    "FIVE",
    "SIX",
    "SEVEN",
    "EIGHT",
    "NINE",
    "TEN",
    "ELEVEN",
    "TWELVE",
    "THIRTEEN",
    "FOURTEEN",
    "FIFTEEN",
    "SIXTEEN",
    "SEVENTEEN",
    "EIGHTEEN",
    "NINETEEN",
  ];
  const tens = [
    "",
    "",
    "TWENTY",
    "THIRTY",
    "FORTY",
    "FIFTY",
    "SIXTY",
    "SEVENTY",
    "EIGHTY",
    "NINETY",
  ];
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

  let result = "";
  if (currency === "TZS") {
    result = "TANZANIAN SHILLINGS " + convertInteger(dollars);
    if (cents > 0) {
      result += " AND " + convertInteger(cents) + " CENTS";
    }
  } else {
    result = "UNITED STATES DOLLARS " + convertInteger(dollars);
    if (cents > 0) {
      result += " AND " + convertInteger(cents) + " CENTS";
    }
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

// Helper to separate a single address text into Box Office and Physical Address
function parseCustomerAddress(addressStr) {
  if (!addressStr) {
    return {
      postal: "N/A",
      physical: "N/A",
    };
  }

  const lines = addressStr.split(/\n+/);
  if (lines.length > 1) {
    const postalIndex = lines.findIndex((l) => /p\.?o\.?\s*box/i.test(l));
    if (postalIndex !== -1) {
      const postal = lines[postalIndex].trim();
      const physical = lines
        .filter((_, i) => i !== postalIndex)
        .join(", ")
        .trim();
      return { postal, physical: physical || "N/A" };
    }
    return {
      postal: lines[0].trim(),
      physical: lines.slice(1).join(", ").trim(),
    };
  }

  const parts = addressStr.split(/,\s*/);
  const postalIndex = parts.findIndex((p) => /p\.?o\.?\s*box/i.test(p));
  if (postalIndex !== -1) {
    const postal = parts[postalIndex].trim();
    const physical = parts
      .filter((_, i) => i !== postalIndex)
      .join(", ")
      .trim();
    return { postal, physical: physical || "N/A" };
  }

  if (/p\.?o\.?\s*box/i.test(addressStr)) {
    return { postal: addressStr.trim(), physical: "N/A" };
  }

  return {
    postal: "P.O. Box Not Provided",
    physical: addressStr.trim(),
  };
}

export function InvoiceModal({ invoice, onClose, onRefresh, userRole }) {
  const [fetching, setFetching] = useState(false);
  const [fullInvoice, setFullInvoice] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [overrideStatus, setOverrideStatus] = useState(invoice?.paymentStatus || "PENDING_PAYMENT");
  const [activeAccounts, setActiveAccounts] = useState([]);

  const isPending =
    invoice?.paymentStatus?.toUpperCase() === "PENDING" ||
    invoice?.paymentStatus?.toUpperCase() === "PENDING_PAYMENT";
  const isPaid = invoice?.paymentStatus?.toUpperCase() === "PAID";

  useEffect(() => {
    if (invoice?.id) {
      setFetching(true);
      setError("");
      getInvoiceById(invoice.id)
        .then((res) => {
          setFullInvoice(res.data || res);
          setFetching(false);
        })
        .catch((err) => {
          console.error(err);
          setError("Failed to load invoice details: " + (err.message || err));
          setFetching(false);
        });
    } else {
      setFullInvoice(invoice);
    }
  }, [invoice]);

  useEffect(() => {
    if ((userRole === "ADMIN" || userRole === "FINANCE") && isPending && invoice?.id) {
      listActivePaymentAccounts()
        .then((res) => {
          setActiveAccounts(res.data || []);
        })
        .catch((err) => console.error("Error loading active accounts", err));
    }
  }, [userRole, isPending, invoice?.id]);

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
      setSuccess(
        `Invoice status successfully overridden to ${overrideStatus === "PENDING_PAYMENT" ? "Pending Payment" : "Paid"}.`,
      );
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to override invoice status.");
    } finally {
      setLoading(false);
    }
  };

  const handleAccountChange = async (accountId) => {
    if (!accountId) return;
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      await updateInvoicePaymentAccount(invoice.id, accountId);
      setSuccess("Invoice payment instructions updated successfully.");
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to update payment instructions.");
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const handleDownloadPDF = () => {
    const element = document.getElementById("invoice-printable-area");
    if (!element) return;

    setLoading(true);
    const opt = {
      margin: 0.3,
      filename: `${currentInvoice.invoiceNumber || "invoice"}.pdf`,
      image: { type: "jpeg", quality: 0.98 },
      html2canvas: { scale: 2, useCORS: true },
      jsPDF: { unit: "in", format: "letter", orientation: "portrait" },
    };

    if (window.html2pdf) {
      window
        .html2pdf()
        .set(opt)
        .from(element)
        .save()
        .then(() => setLoading(false));
    } else {
      const script = document.createElement("script");
      script.src =
        "https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js";
      script.onload = () => {
        window
          .html2pdf()
          .set(opt)
          .from(element)
          .save()
          .then(() => setLoading(false));
      };
      document.body.appendChild(script);
    }
  };

  const currentInvoice = fullInvoice || invoice;
  const order = currentInvoice.order || {};
  const customer = order.customer || {};
  const product = order.product || {};
  const companyDetails = currentInvoice.companyDetails || {};

  if (typeof window === "undefined") return null;

  const currency = order.currency || product.currency || "USD";
  const currencySymbol = currency === "TZS" ? "TZS" : "US $";

  // Dynamic specifications, descriptions, categories
  const fuelCategory = currentInvoice.fuelCategory || product.fuelCategory || "";
  const productSpecification = currentInvoice.productSpecification || product.specification || "";
  const productDescription = currentInvoice.productDescription || product.description || "";
  const unit = currentInvoice.unitOfMeasurement || product.unitOfMeasurement || "Litres";

  // Calculations
  const quantityVal = order.quantity || 0;
  const unitPriceVal = product.unitPrice || 0;
  const baseAmount = quantityVal * unitPriceVal;

  const levies = currentInvoice.levies || order.levies || 0;
  const discount = currentInvoice.discount || order.discount || 0;
  const transportCharges = currentInvoice.transportCharges || order.transportCharges || 0;
  const deliveryCharges = currentInvoice.deliveryCharges || order.deliveryCharges || 0;
  const additionalCharges = currentInvoice.additionalCharges || order.additionalCharges || 0;

  const subtotal =
    currentInvoice.subtotal ||
    baseAmount + levies + transportCharges + deliveryCharges + additionalCharges - discount;
  const tax = currentInvoice.tax || subtotal * 0.16;
  const grandTotal = currentInvoice.grandTotal || subtotal + tax;
  const totalAmountWords = numberToWords(grandTotal, currency);

  const parsedAddress = parseCustomerAddress(customer.address || order.locationAddress);

  // Delivery & Incoterms details
  const deliveryMethod = currentInvoice.deliveryMethod || order.deliveryMethod || "";
  const incoterms = currentInvoice.incoterms || order.incoterms || "";
  const port = currentInvoice.port || order.port || "";
  const destination = currentInvoice.destination || order.destination || "";
  const logisticsInfo = currentInvoice.logisticsInfo || order.logisticsInfo || "";
  const physicalLocation = order.locationAddress || customer.address || "";

  const hasDeliveryDetails =
    deliveryMethod || incoterms || port || destination || logisticsInfo || physicalLocation;

  // Signatures and logos
  const logoUrl = companyDetails.logo || logoImg;
  const signatureUrl = companyDetails.signatorySignature || signatureImg;
  const stampUrl = companyDetails.stamp || stampImg;

  // UI Validation: check if required database records are missing
  const validationErrors = [];
  if (!customer.companyName && !customer.contactPerson) {
    validationErrors.push("Customer details (name or company name) are missing in the system.");
  }
  if (!product.productName) {
    validationErrors.push("Fuel product reference is missing.");
  }
  if (quantityVal <= 0) {
    validationErrors.push("Order quantity must be a positive number.");
  }
  if (unitPriceVal <= 0) {
    validationErrors.push("Approved fuel unit price is missing or invalid.");
  }
  if (!currency) {
    validationErrors.push("Currency configuration is missing.");
  }
  if (!currentInvoice.accountNumber || !currentInvoice.bankName) {
    validationErrors.push("Finance-managed active payment account instructions are missing.");
  }

  return createPortal(
    <div className="fef-modal-backdrop" onClick={onClose} style={{ zIndex: 9999 }}>
      <div
        className="fef-modal-window"
        style={{
          maxWidth: "750px",
          width: "100%",
          maxHeight: "90vh",
          display: "flex",
          flexDirection: "column",
          padding: 0,
          overflow: "hidden",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Sticky Modal Header */}
        <div
          className="no-print"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            padding: "16px 24px",
            borderBottom: "1px solid rgba(255,255,255,0.1)",
            background: "rgba(30, 41, 59, 0.95)",
          }}
        >
          <h3
            style={{
              margin: 0,
              fontSize: "18px",
              fontWeight: "600",
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            <span>{currentInvoice.invoiceType || "Invoice"} Details</span>
            <span
              className={`fef-badge fef-badge-${currentInvoice.paymentStatus?.toLowerCase()}`}
              style={{ fontSize: "12px", padding: "2px 8px" }}
            >
              {currentInvoice.paymentStatus === "PENDING_PAYMENT"
                ? "Pending Payment"
                : currentInvoice.paymentStatus === "PAID"
                  ? "Paid"
                  : currentInvoice.paymentStatus}
            </span>
          </h3>
          <div style={{ display: "flex", gap: "8px" }}>
            <button
              className="fef-btn fef-btn-outline no-print"
              onClick={handlePrint}
              style={{
                padding: "6px 12px",
                display: "flex",
                alignItems: "center",
                gap: "6px",
                fontSize: "13px",
              }}
            >
              <FiPrinter /> Print
            </button>
            <button
              className="fef-btn fef-btn-outline no-print"
              onClick={handleDownloadPDF}
              style={{
                padding: "6px 12px",
                display: "flex",
                alignItems: "center",
                gap: "6px",
                fontSize: "13px",
              }}
              disabled={loading}
            >
              <FiDownload /> Download PDF
            </button>
            <button
              className="fef-modal-close"
              onClick={onClose}
              style={{
                position: "static",
                background: "none",
                border: "none",
                color: "var(--feftms-text-muted)",
                cursor: "pointer",
              }}
            >
              <FiX size={20} />
            </button>
          </div>
        </div>

        {/* Modal Scrollable Content */}
        <div
          className="fef-modal-scrollable-content"
          style={{
            overflowY: "auto",
            padding: "24px",
            flex: 1,
            background: "#f8fafc",
            color: "#1e293b",
          }}
        >
          {fetching ? (
            <div
              style={{
                padding: "40px 0",
                textAlign: "center",
                color: "#1e293b",
                fontWeight: "bold",
              }}
            >
              <div
                className="fef-spinner"
                style={{
                  margin: "0 auto 12px auto",
                  width: 40,
                  height: 40,
                  border: "4px solid rgba(30,41,59,0.1)",
                  borderTopColor: "#1e293b",
                  borderRadius: "50%",
                  animation: "spin 1s linear infinite",
                }}
              />
              Loading real system invoice data...
            </div>
          ) : validationErrors.length > 0 ? (
            <div className="fef-alert fef-alert-danger fef-fade-in" style={{ padding: 20 }}>
              <h4
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "6px",
                  margin: "0 0 10px 0",
                  fontWeight: "600",
                  fontSize: "16px",
                }}
              >
                <FiAlertCircle /> Required Invoice Information is Missing
              </h4>
              <p style={{ margin: "0 0 12px 0", fontSize: "13px" }}>
                This invoice cannot be generated or displayed because some required records are
                missing in the database. Please complete the fields in the system first.
              </p>
              <ul style={{ margin: 0, paddingLeft: "20px", fontSize: "13px" }}>
                {validationErrors.map((err, i) => (
                  <li key={i} style={{ marginBottom: 4 }}>
                    {err}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <>
              {error && (
                <div
                  className="fef-alert fef-alert-danger fef-fade-in"
                  style={{ marginBottom: 20 }}
                >
                  <FiAlertCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
                  {error}
                </div>
              )}

              {success && (
                <div
                  className="fef-alert fef-alert-success fef-fade-in"
                  style={{ marginBottom: 20 }}
                >
                  <FiCheckCircle style={{ verticalAlign: "-2px", marginRight: 6 }} />
                  {success}
                </div>
              )}

              {/* Payment Account Selector for Admin / Finance */}
              {isPending &&
                (userRole === "ADMIN" || userRole === "FINANCE") &&
                activeAccounts.length > 0 && (
                  <div
                    className="fef-panel no-print"
                    style={{
                      marginBottom: 20,
                      padding: 15,
                      background: "rgba(30, 41, 59, 0.05)",
                      border: "1px solid rgba(0,0,0,0.1)",
                      borderRadius: 6,
                    }}
                  >
                    <label
                      style={{ fontWeight: "600", fontSize: 13, display: "block", marginBottom: 6 }}
                    >
                      Select Approved Payment Account instructions:
                    </label>
                    <select
                      value={currentInvoice.paymentAccountId || ""}
                      onChange={(e) => handleAccountChange(e.target.value)}
                      style={{
                        width: "100%",
                        padding: "8px 12px",
                        borderRadius: "6px",
                        background: "#ffffff",
                        color: "#1e293b",
                        border: "1px solid rgba(0,0,0,0.2)",
                        fontSize: "13px",
                      }}
                      disabled={loading}
                    >
                      <option value="">-- Select Active Payment Account --</option>
                      {activeAccounts.map((acc) => (
                        <option key={acc.id} value={acc.id}>
                          {acc.bankName} - {acc.currency} ({acc.accountNumber}) -{" "}
                          {acc.paymentMethod}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

              <div id="invoice-printable-area">
                <div
                  className="invoice-card"
                  style={{
                    background: "#ffffff",
                    color: "#000000",
                    border: "3px double #000000",
                    borderRadius: "8px",
                    padding: "40px",
                    boxShadow:
                      "0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)",
                    fontFamily: "Arial, sans-serif",
                  }}
                >
                  {/* Header: Logo and Company Info */}
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "20px",
                    }}
                  >
                    <div>
                      <img
                        src={logoUrl}
                        alt="Falcon Energy Logo"
                        style={{ height: "90px", width: "auto" }}
                      />
                    </div>
                    <div
                      style={{
                        textAlign: "right",
                        fontSize: "13px",
                        lineHeight: "1.5",
                        color: "#000000",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "20px",
                          fontWeight: "bold",
                          color: "#000000",
                          letterSpacing: "0.5px",
                          marginBottom: "4px",
                        }}
                      >
                        {companyDetails.companyName || "FALCON ENERGY LIMITED"}
                      </div>
                      <div style={{ fontWeight: "600" }}>{companyDetails.postalAddress}</div>
                      <div style={{ fontWeight: "600" }}>{companyDetails.officeAddress}</div>
                      {companyDetails.phoneNumber && (
                        <div style={{ fontWeight: "600" }}>Tel: {companyDetails.phoneNumber}</div>
                      )}
                      {companyDetails.email && (
                        <div style={{ fontWeight: "600" }}>Email: {companyDetails.email}</div>
                      )}
                    </div>
                  </div>

                  {/* Document Title */}
                  <div
                    style={{
                      textAlign: "center",
                      fontSize: "20px",
                      fontWeight: "bold",
                      borderTop: "1px solid #000000",
                      borderBottom: "1px solid #000000",
                      padding: "6px 0",
                      marginBottom: "20px",
                      letterSpacing: "1px",
                      color: "#000000",
                    }}
                  >
                    {(currentInvoice.invoiceType || "Proforma Invoice").toUpperCase()}
                  </div>

                  {/* Order adjustment warning if present */}
                  {order.originalQuantity && order.originalQuantity !== order.quantity && (
                    <div
                      className="no-print"
                      style={{
                        background: "#fffbeb",
                        border: "1px solid #fef3c7",
                        borderRadius: "6px",
                        padding: "8px 12px",
                        marginBottom: "15px",
                        fontSize: "12px",
                        color: "#92400e",
                      }}
                    >
                      <strong>⚠️ Order Adjustment Audit Trail:</strong>
                      <span style={{ marginLeft: "8px" }}>
                        Original: {order.originalQuantity?.toLocaleString()} L | Adjusted:{" "}
                        {order.quantity?.toLocaleString()} L
                        {order.editReason && ` (${order.editReason})`}
                      </span>
                    </div>
                  )}

                  {/* Main Invoice Table */}
                  <table
                    style={{
                      width: "100%",
                      borderCollapse: "collapse",
                      border: "1px solid #000000",
                      fontSize: "13px",
                      color: "#000000",
                    }}
                  >
                    <tbody>
                      {/* Row 1: Date & Invoice No */}
                      <tr>
                        <td
                          style={{
                            width: "15%",
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          DATE:
                        </td>
                        <td
                          style={{ width: "45%", border: "1px solid #000000", padding: "8px 12px" }}
                        >
                          {formatDateLong(currentInvoice.invoiceDate)}
                        </td>
                        <td
                          style={{
                            width: "15%",
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          INVOICE No:
                        </td>
                        <td
                          style={{
                            width: "25%",
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          {currentInvoice.invoiceNumber}
                        </td>
                      </tr>

                      {/* Row 2: Buyer Details */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                            verticalAlign: "top",
                          }}
                        >
                          BUYER:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            verticalAlign: "top",
                          }}
                        >
                          <div style={{ fontWeight: "bold", marginBottom: "4px" }}>
                            {customer.companyName || customer.contactPerson}
                          </div>
                          {parsedAddress.postal !== "N/A" && (
                            <div style={{ marginBottom: "2px" }}>{parsedAddress.postal}</div>
                          )}
                          {parsedAddress.physical !== "N/A" && (
                            <div style={{ marginBottom: "4px" }}>{parsedAddress.physical}</div>
                          )}
                          {customer.phone && (
                            <div style={{ fontSize: "12px", color: "#475569" }}>
                              <strong>Phone:</strong> {customer.phone}
                            </div>
                          )}
                          {customer.email && (
                            <div style={{ fontSize: "12px", color: "#475569" }}>
                              <strong>Email:</strong> {customer.email}
                            </div>
                          )}
                          {customer.tinNumber && (
                            <div style={{ fontSize: "12px", color: "#475569" }}>
                              <strong>TIN:</strong> {customer.tinNumber}
                            </div>
                          )}
                        </td>
                      </tr>

                      {/* Row 3: Product Description */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                            verticalAlign: "top",
                          }}
                        >
                          PRODUCT:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          {product.productName} {fuelCategory && `(${fuelCategory})`}
                          <br />
                          {productSpecification && (
                            <span
                              style={{
                                fontSize: "11px",
                                fontWeight: "normal",
                                display: "block",
                                marginTop: "4px",
                                color: "#475569",
                              }}
                            >
                              {productSpecification}
                            </span>
                          )}
                          {productDescription && (
                            <span
                              style={{
                                fontSize: "11px",
                                fontWeight: "normal",
                                display: "block",
                                color: "#64748b",
                              }}
                            >
                              {productDescription}
                            </span>
                          )}
                        </td>
                      </tr>

                      {/* Row 4: Quantity */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          QUANTITY:
                        </td>
                        <td
                          colSpan={3}
                          style={{ border: "1px solid #000000", padding: "8px 12px" }}
                        >
                          {quantityVal.toLocaleString()} {unit}
                          {unit.toLowerCase() === "litres" &&
                            ` (${(quantityVal / 1000).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 })} CBM)`}
                        </td>
                      </tr>

                      {/* Row 5: Price */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          UNIT PRICE:
                        </td>
                        <td
                          colSpan={3}
                          style={{ border: "1px solid #000000", padding: "8px 12px" }}
                        >
                          {currencySymbol} {unitPriceVal.toFixed(2)} Per{" "}
                          {unit.slice(0, -1) || "Litre"}
                          {unit.toLowerCase() === "litres" &&
                            ` (${currencySymbol} ${(unitPriceVal * 1000).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Per CBM)`}
                        </td>
                      </tr>

                      {/* Dynamic calculations list if transport charges/levies are present */}
                      {(levies > 0 ||
                        transportCharges > 0 ||
                        deliveryCharges > 0 ||
                        additionalCharges > 0 ||
                        discount > 0) && (
                        <>
                          <tr>
                            <td
                              style={{
                                border: "1px solid #000000",
                                padding: "8px 12px",
                                fontWeight: "bold",
                              }}
                            >
                              FUEL COST:
                            </td>
                            <td
                              colSpan={3}
                              style={{ border: "1px solid #000000", padding: "8px 12px" }}
                            >
                              {currencySymbol}{" "}
                              {baseAmount.toLocaleString(undefined, {
                                minimumFractionDigits: 2,
                                maximumFractionDigits: 2,
                              })}
                            </td>
                          </tr>
                          {levies > 0 && (
                            <tr>
                              <td
                                style={{
                                  border: "1px solid #000000",
                                  padding: "8px 12px",
                                  fontWeight: "bold",
                                }}
                              >
                                LEVIES & TAXES:
                              </td>
                              <td
                                colSpan={3}
                                style={{ border: "1px solid #000000", padding: "8px 12px" }}
                              >
                                {currencySymbol}{" "}
                                {levies.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}
                              </td>
                            </tr>
                          )}
                          {transportCharges > 0 && (
                            <tr>
                              <td
                                style={{
                                  border: "1px solid #000000",
                                  padding: "8px 12px",
                                  fontWeight: "bold",
                                }}
                              >
                                TRANSPORT:
                              </td>
                              <td
                                colSpan={3}
                                style={{ border: "1px solid #000000", padding: "8px 12px" }}
                              >
                                {currencySymbol}{" "}
                                {transportCharges.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}
                              </td>
                            </tr>
                          )}
                          {deliveryCharges > 0 && (
                            <tr>
                              <td
                                style={{
                                  border: "1px solid #000000",
                                  padding: "8px 12px",
                                  fontWeight: "bold",
                                }}
                              >
                                DELIVERY FEE:
                              </td>
                              <td
                                colSpan={3}
                                style={{ border: "1px solid #000000", padding: "8px 12px" }}
                              >
                                {currencySymbol}{" "}
                                {deliveryCharges.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}
                              </td>
                            </tr>
                          )}
                          {additionalCharges > 0 && (
                            <tr>
                              <td
                                style={{
                                  border: "1px solid #000000",
                                  padding: "8px 12px",
                                  fontWeight: "bold",
                                }}
                              >
                                ADDITIONAL CHARGES:
                              </td>
                              <td
                                colSpan={3}
                                style={{ border: "1px solid #000000", padding: "8px 12px" }}
                              >
                                {currencySymbol}{" "}
                                {additionalCharges.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}
                              </td>
                            </tr>
                          )}
                          {discount > 0 && (
                            <tr>
                              <td
                                style={{
                                  border: "1px solid #000000",
                                  padding: "8px 12px",
                                  fontWeight: "bold",
                                  color: "#b91c1c",
                                }}
                              >
                                DISCOUNT APPLIED:
                              </td>
                              <td
                                colSpan={3}
                                style={{
                                  border: "1px solid #000000",
                                  padding: "8px 12px",
                                  color: "#b91c1c",
                                }}
                              >
                                - {currencySymbol}{" "}
                                {discount.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}
                              </td>
                            </tr>
                          )}
                        </>
                      )}

                      {/* Row 6: Subtotal */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          SUBTOTAL:
                        </td>
                        <td
                          colSpan={3}
                          style={{ border: "1px solid #000000", padding: "8px 12px" }}
                        >
                          {currencySymbol}{" "}
                          {subtotal.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </td>
                      </tr>

                      {/* VAT */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          VAT (16%):
                        </td>
                        <td
                          colSpan={3}
                          style={{ border: "1px solid #000000", padding: "8px 12px" }}
                        >
                          {currencySymbol}{" "}
                          {tax.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </td>
                      </tr>

                      {/* Row 7: Grand Total Numeric */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          GRAND TOTAL:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                            fontSize: "14px",
                          }}
                        >
                          {currencySymbol}{" "}
                          {grandTotal.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </td>
                      </tr>

                      {/* Row 8: Grand Total Words */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                            verticalAlign: "top",
                          }}
                        >
                          TOTAL AMOUNT:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          ({totalAmountWords})
                        </td>
                      </tr>

                      {/* Optional Delivery details */}
                      {hasDeliveryDetails && (
                        <tr>
                          <td
                            style={{
                              border: "1px solid #000000",
                              padding: "8px 12px",
                              fontWeight: "bold",
                              verticalAlign: "top",
                            }}
                          >
                            DELIVERY:
                          </td>
                          <td
                            colSpan={3}
                            style={{ border: "1px solid #000000", padding: "8px 12px" }}
                          >
                            {deliveryMethod && (
                              <div style={{ marginBottom: 2 }}>
                                <strong>Delivery Method:</strong> {deliveryMethod}
                              </div>
                            )}
                            {incoterms && (
                              <div style={{ marginBottom: 2 }}>
                                <strong>Incoterms:</strong> {incoterms}
                              </div>
                            )}
                            {port && (
                              <div style={{ marginBottom: 2 }}>
                                <strong>Loading Port:</strong> {port}
                              </div>
                            )}
                            {destination && (
                              <div style={{ marginBottom: 2 }}>
                                <strong>Destination:</strong> {destination}
                              </div>
                            )}
                            {physicalLocation && (
                              <div style={{ marginBottom: 2 }}>
                                <strong>Delivery Location:</strong> {physicalLocation}
                              </div>
                            )}
                            {logisticsInfo && (
                              <div style={{ fontSize: "12px", color: "#64748b", marginTop: 4 }}>
                                <strong>Logistics:</strong> {logisticsInfo}
                              </div>
                            )}
                          </td>
                        </tr>
                      )}

                      {/* Row 9: Payment Instructions Header */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          PAYMENT TERMS:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          {currentInvoice.paymentTerms || "PREPAYMENT"}
                        </td>
                      </tr>

                      {/* Row 10: Bank Title */}
                      <tr>
                        <td
                          colSpan={4}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                            textDecoration: "underline",
                          }}
                        >
                          PLEASE PAY THE ABOVE AMOUNT BY{" "}
                          {currentInvoice.paymentMethod?.toUpperCase() || "TT"} TO:
                        </td>
                      </tr>

                      {/* Row 11: Bank Table & Stamp Split */}
                      <tr>
                        <td
                          colSpan={2}
                          style={{
                            width: "60%",
                            border: "1px solid #000000",
                            padding: 0,
                            verticalAlign: "top",
                          }}
                        >
                          <table
                            style={{ width: "100%", borderCollapse: "collapse", fontSize: "12px" }}
                          >
                            <tbody>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td
                                  style={{
                                    width: "30%",
                                    padding: "6px 8px",
                                    borderRight: "1px solid #000000",
                                    fontWeight: "bold",
                                  }}
                                >
                                  Beneficiary:
                                </td>
                                <td
                                  style={{ width: "70%", padding: "6px 8px", fontWeight: "bold" }}
                                >
                                  {currentInvoice.beneficiaryName || "FALCON ENERGY LIMITED"}
                                </td>
                              </tr>
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td
                                  style={{
                                    padding: "6px 8px",
                                    borderRight: "1px solid #000000",
                                    fontWeight: "bold",
                                  }}
                                >
                                  Bank Name:
                                </td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>
                                  {currentInvoice.bankName}
                                </td>
                              </tr>
                              {currentInvoice.branchName && (
                                <tr style={{ borderBottom: "1px solid #000000" }}>
                                  <td
                                    style={{
                                      padding: "6px 8px",
                                      borderRight: "1px solid #000000",
                                      fontWeight: "bold",
                                    }}
                                  >
                                    Branch Name:
                                  </td>
                                  <td style={{ padding: "6px 8px", fontWeight: "bold" }}>
                                    {currentInvoice.branchName}
                                  </td>
                                </tr>
                              )}
                              <tr style={{ borderBottom: "1px solid #000000" }}>
                                <td
                                  style={{
                                    padding: "6px 8px",
                                    borderRight: "1px solid #000000",
                                    fontWeight: "bold",
                                  }}
                                >
                                  Account No:
                                </td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>
                                  {currentInvoice.accountNumber}
                                </td>
                              </tr>
                              {currentInvoice.swiftCode && (
                                <tr style={{ borderBottom: "1px solid #000000" }}>
                                  <td
                                    style={{
                                      padding: "6px 8px",
                                      borderRight: "1px solid #000000",
                                      fontWeight: "bold",
                                    }}
                                  >
                                    Swift Code:
                                  </td>
                                  <td style={{ padding: "6px 8px", fontWeight: "bold" }}>
                                    {currentInvoice.swiftCode}
                                  </td>
                                </tr>
                              )}
                              <tr>
                                <td
                                  style={{
                                    padding: "6px 8px",
                                    borderRight: "1px solid #000000",
                                    fontWeight: "bold",
                                  }}
                                >
                                  Currency:
                                </td>
                                <td style={{ padding: "6px 8px", fontWeight: "bold" }}>
                                  {currentInvoice.paymentAccountCurrency === "TZS"
                                    ? "Tanzanian Shillings"
                                    : "United States Dollars"}
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </td>
                        <td
                          colSpan={2}
                          style={{
                            width: "40%",
                            border: "1px solid #000000",
                            padding: "10px",
                            textAlign: "center",
                            verticalAlign: "middle",
                          }}
                        >
                          {stampUrl && (
                            <img
                              src={stampUrl}
                              alt="Falcon Energy Limited Stamp"
                              style={{ height: "115px", width: "auto", display: "inline-block" }}
                            />
                          )}
                        </td>
                      </tr>

                      {/* Row 12: Validity */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          PLEASE NOTE:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          {currentInvoice.paymentInstructions}
                          {currentInvoice.validityDate && (
                            <div style={{ fontSize: "11px", color: "#64748b", marginTop: 4 }}>
                              Valid Until: {formatDateLong(currentInvoice.validityDate)}
                            </div>
                          )}
                        </td>
                      </tr>

                      {/* Row 13: FOR */}
                      <tr>
                        <td
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          FOR:
                        </td>
                        <td
                          colSpan={3}
                          style={{
                            border: "1px solid #000000",
                            padding: "8px 12px",
                            fontWeight: "bold",
                          }}
                        >
                          {companyDetails.companyName?.toUpperCase() || "FALCON ENERGY LIMITED"}
                        </td>
                      </tr>

                      {/* Row 14: Signature */}
                      <tr>
                        <td
                          colSpan={4}
                          style={{ border: "1px solid #000000", padding: "6px 12px" }}
                        >
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                              width: "100%",
                            }}
                          >
                            <div
                              style={{
                                fontWeight: "bold",
                                textTransform: "uppercase",
                                fontSize: "12px",
                                width: "50%",
                                textAlign: "center",
                              }}
                            >
                              {companyDetails.signatoryName || "AUTHORIZED SIGNATORY"}
                              <br />
                              <span
                                style={{ fontSize: "10px", fontWeight: "normal", color: "#475569" }}
                              >
                                {companyDetails.signatoryTitle || "FINANCE CONTROLLER"}
                              </span>
                            </div>
                            <div style={{ width: "50%", textAlign: "right", paddingRight: "40px" }}>
                              {signatureUrl && (
                                <img
                                  src={signatureUrl}
                                  alt="Authorized Signature"
                                  style={{ height: "45px", width: "auto", display: "inline-block" }}
                                />
                              )}
                            </div>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>

                  {/* Payment confirmation section */}
                  {isPaid && (
                    <div
                      style={{
                        marginTop: 20,
                        padding: 15,
                        background: "#f0fdf4",
                        border: "1px solid #bbf7d0",
                        borderRadius: 6,
                        fontSize: "13px",
                      }}
                    >
                      <div
                        style={{
                          fontWeight: "bold",
                          color: "#166534",
                          display: "flex",
                          alignItems: "center",
                          gap: "6px",
                        }}
                      >
                        <FiCheckCircle /> Payment Confirmed
                      </div>
                      {currentInvoice.financeApprovedBy && (
                        <div>Confirmed By: {currentInvoice.financeApprovedBy}</div>
                      )}
                      {currentInvoice.financeApprovedAt && (
                        <div>
                          Confirmation Date: {formatDateLong(currentInvoice.financeApprovedAt)}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>

        {/* Sticky Modal Footer: Action Bar */}
        <div
          className="no-print"
          style={{
            display: "flex",
            justifyContent: "flex-end",
            alignItems: "center",
            gap: "12px",
            padding: "16px 24px",
            borderTop: "1px solid rgba(255,255,255,0.1)",
            background: "rgba(30, 41, 59, 0.95)",
          }}
        >
          <button className="fef-btn fef-btn-outline" onClick={onClose} disabled={loading}>
            Close
          </button>

          {isPending && userRole === "FINANCE" && validationErrors.length === 0 && (
            <button
              className="fef-btn fef-btn-success"
              onClick={handleApprovePayment}
              disabled={loading}
              style={{ display: "flex", alignItems: "center", gap: "6px" }}
            >
              <FiCheck /> {loading ? "Processing..." : "Confirm Payment"}
            </button>
          )}

          {userRole === "ADMIN" && validationErrors.length === 0 && (
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
                  fontSize: "13px",
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

      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>,
    document.body,
  );
}
