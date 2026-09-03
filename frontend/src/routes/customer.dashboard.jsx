import { createFileRoute } from "@tanstack/react-router";
import { Fragment, useEffect, useState } from "react";
import {
  FiHome,
  FiClipboard,
  FiPlusCircle,
  FiFileText,
  FiTruck,
  FiUser,
  FiDownload,
  FiAlertCircle,
  FiMapPin,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { useAuth } from "../context/AuthContext";
import {
  getCustomerDashboard,
  listCustomerOrders,
  createCustomerOrder,
  previewTransportRoute,
  getCustomerTimeline,
  getCustomerDocuments,
  listCustomerInvoices,
  listCustomerDeliveries,
  getCustomerDeliveryTracking,
  getCustomerProfile,
  updateCustomerProfile,
  downloadCustomerDocument,
  initiateInvoicePayment,
  listInvoicePayments,
  refreshCustomerPayment,
  cancelCustomerPayment,
} from "../services/customerPortalService";
import { OpenStreetMapLocationPicker } from "../components/OpenStreetMapLocationPicker";
import { DeliveryTrackingMap } from "../components/DeliveryTrackingMap";
import { listProducts } from "../services/productService";
import { reverseGeocode } from "../services/geocodingService";
import "../styles/forms.css";

export const Route = createFileRoute("/customer/dashboard")({ component: CustomerDashboard });
const SIDE = [
  { key: "dashboard", label: "Dashboard", icon: FiHome },
  { key: "orders", label: "Orders", icon: FiClipboard },
  { key: "new", label: "Place Order", icon: FiPlusCircle },
  { key: "invoices", label: "Invoices", icon: FiFileText },
  { key: "deliveries", label: "Deliveries", icon: FiTruck },
  { key: "profile", label: "Profile", icon: FiUser },
];
const money = (v) => Number(v || 0).toLocaleString();
const paymentLabel = (status) => ({
  INITIATED: "Payment request sent",
  PENDING: "Waiting for payment approval",
  PROCESSING: "Payment is being processed",
  ACTION_REQUIRED: "Approve payment to continue",
  SUCCESSFUL: "Payment completed successfully",
  FAILED: "Payment failed",
  CANCELLED: "Payment was cancelled",
  EXPIRED: "Payment expired",
  UNKNOWN: "Payment status needs checking",
}[status] || status || "—");
function CustomerDashboard() {
  return (
    <RouteGuard allowedRoles={["CUSTOMER"]}>
      <CustomerWorkspace />
    </RouteGuard>
  );
}
function CustomerWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState("dashboard"),
    [data, setData] = useState(null),
    [orders, setOrders] = useState([]),
    [invoices, setInvoices] = useState([]),
    [deliveries, setDeliveries] = useState([]),
    [products, setProducts] = useState([]),
    [profile, setProfile] = useState(null),
    [selected, setSelected] = useState(null),
    [timeline, setTimeline] = useState(null),
    [deliveryDocuments, setDeliveryDocuments] = useState({}),
    [expandedDeliveryId, setExpandedDeliveryId] = useState(null),
    [loadingDeliveryDocuments, setLoadingDeliveryDocuments] = useState(null),
    [trackingDeliveryId, setTrackingDeliveryId] = useState(null),
    [tracking, setTracking] = useState(null),
    [trackingPlace, setTrackingPlace] = useState(""),
    [trackingError, setTrackingError] = useState(""),
    [error, setError] = useState(""),
    [success, setSuccess] = useState(""),
    [selectedInvoice, setSelectedInvoice] = useState(null),
    [paymentState, setPaymentState] = useState(""),
    [paymentHistory, setPaymentHistory] = useState([]),
    [paying, setPaying] = useState(false),
    [paymentMethod, setPaymentMethod] = useState("AIRTEL_MONEY"),
    [paymentPhone, setPaymentPhone] = useState(import.meta.env.DEV ? "0683456789" : ""),
    [location, setLocation] = useState(null),
    [routePreview, setRoutePreview] = useState(null),
    [routeLoading, setRouteLoading] = useState(false);
  const refresh = async () => {
    try {
      const [d, o, i, dl, p, pr] = await Promise.all([
        getCustomerDashboard(),
        listCustomerOrders(),
        listCustomerInvoices(),
        listCustomerDeliveries(),
        listProducts({ size: 100 }),
        getCustomerProfile(),
      ]);
      setData(d);
      setOrders(o || []);
      setInvoices(i || []);
      setSelectedInvoice((current) =>
        current ? (i || []).find((invoice) => invoice.id === current.id) || current : current,
      );
      setDeliveries(dl || []);
      setProducts((p.content || []).map((x) => ({ ...x, availableQuantity: undefined })));
      setProfile(pr);
    } catch (e) {
      setError(e.message || "Unable to load your portal.");
    }
  };
  useEffect(() => {
    refresh();
  }, []);

  // Flutterwave returns to this route after the sandbox authorization page. The
  // payment ID is stored before redirecting so the result is checked without the
  // customer needing to reopen the invoice or press Pay again.
  useEffect(() => {
    const paymentId = sessionStorage.getItem("flw_payment_return_id");
    const invoiceId = sessionStorage.getItem("flw_payment_return_invoice_id");
    if (!paymentId || !invoiceId) return undefined;
    let active = true;
    let attempts = 0;
    const checkReturnedPayment = async () => {
      try {
        const payment = await refreshCustomerPayment(paymentId);
        if (!active) return;
        const history = (await listInvoicePayments(invoiceId)) || [];
        setPaymentHistory(history);
        await refresh();
        if (["SUCCESSFUL", "COMPLETED", "FAILED", "CANCELLED", "EXPIRED"].includes(payment.status)) {
          sessionStorage.removeItem("flw_payment_return_id");
          sessionStorage.removeItem("flw_payment_return_invoice_id");
          if (["SUCCESSFUL", "COMPLETED"].includes(payment.status)) setSuccess("Payment completed successfully. Your invoice is now paid.");
          return;
        }
      } catch (e) {
        if (active) setError(e.message || "Unable to check the returned payment.");
      }
      if (active && attempts++ < 10) window.setTimeout(checkReturnedPayment, 3_000);
    };
    checkReturnedPayment();
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!trackingDeliveryId) return undefined;
    let active = true;
    const loadTracking = async () => {
      try {
        const next = await getCustomerDeliveryTracking(trackingDeliveryId);
        if (!active) return;
        setTracking(next);
        setTrackingError("");
        if (next?.live) {
          try {
            const place = await reverseGeocode(next.latitude, next.longitude);
            if (active) setTrackingPlace(place?.address || "Current driver location");
          } catch {
            if (active) setTrackingPlace("Current driver location");
          }
        } else if (active) setTrackingPlace("");
      } catch (trackingRequestError) {
        if (active) setTrackingError(trackingRequestError.message || "Unable to refresh live tracking.");
      }
    };
    loadTracking();
    const interval = window.setInterval(loadTracking, 20_000);
    return () => { active = false; window.clearInterval(interval); };
  }, [trackingDeliveryId]);
  useEffect(() => {
    if (tab !== "invoices") return undefined;
    const refreshInterval = window.setInterval(refresh, 15000);
    return () => window.clearInterval(refreshInterval);
  }, [tab]);
  useEffect(() => {
    if (!selectedInvoice || !paymentHistory.some((p) => ["INITIATED", "PENDING", "PROCESSING", "ACTION_REQUIRED", "UNKNOWN"].includes(p.status))) return undefined;
    let attempts = 0;
    const timer = window.setInterval(async () => {
      const active = paymentHistory.find((p) => ["INITIATED", "PENDING", "PROCESSING", "ACTION_REQUIRED", "UNKNOWN"].includes(p.status));
      if (!active || attempts++ >= 6) return window.clearInterval(timer);
      try { await refreshCustomerPayment(active.id); setPaymentHistory((await listInvoicePayments(selectedInvoice.id)) || []); await refresh(); } catch { /* Manual Check Status remains available. */ }
    }, 10_000);
    return () => window.clearInterval(timer);
  }, [selectedInvoice?.id, paymentHistory]);
  const detail = async (o) => {
    setSelected(o);
    try {
      const t = await getCustomerTimeline(o.id);
      setTimeline(t);
    } catch (e) {
      setError(e.message);
    }
  };
  const toggleDeliveryDocuments = async (delivery) => {
    if (expandedDeliveryId === delivery.id) {
      setExpandedDeliveryId(null);
      return;
    }
    setExpandedDeliveryId(delivery.id);
    if (deliveryDocuments[delivery.id] !== undefined || !delivery.orderId) return;
    try {
      setLoadingDeliveryDocuments(delivery.id);
      const documents = await getCustomerDocuments(delivery.orderId);
      setDeliveryDocuments((current) => ({ ...current, [delivery.id]: documents || [] }));
    } catch (e) {
      setError(e.message || "Unable to load delivery documents.");
      setDeliveryDocuments((current) => ({ ...current, [delivery.id]: [] }));
    } finally {
      setLoadingDeliveryDocuments(null);
    }
  };
  const toggleLiveTracking = (delivery) => {
    if (trackingDeliveryId === delivery.id) {
      setTrackingDeliveryId(null);
      setTracking(null);
      setTrackingPlace("");
      setTrackingError("");
      return;
    }
    setTrackingDeliveryId(delivery.id);
    setTracking(null);
    setTrackingPlace("");
    setTrackingError("");
  };
  const download = async (path, name) => {
    try {
      const blob = await downloadCustomerDocument(path);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = name;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e.message || "Document download failed.");
    }
  };
  const selectLocation = async (next) => {
    setLocation(next);
    setRoutePreview(null);
    setError("");
    setRouteLoading(true);
    try {
      setRoutePreview(
        await previewTransportRoute({ latitude: next.latitude, longitude: next.longitude }),
      );
    } catch (e) {
      setError(
        e.message ||
          "Unable to calculate transport cost for this destination. Please select another location or try again.",
      );
    } finally {
      setRouteLoading(false);
    }
  };
  const submit = async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    if (!location || !routePreview) {
      setError("Select a delivery location with a calculated driving route before submitting.");
      return;
    }
    try {
      await createCustomerOrder({
        productId: Number(fd.get("productId")),
        quantity: Number(fd.get("quantity")),
        deliveryAddress: location.address,
        destination: location.address,
        deliveryLatitude: location.latitude,
        deliveryLongitude: location.longitude,
        notes: fd.get("notes"),
      });
      setSuccess("Your order was submitted for Sales review.");
      form.reset();
      setLocation(null);
      setRoutePreview(null);
      setTab("orders");
      refresh();
    } catch (e) {
      setError(e.message || "Order could not be submitted.");
    }
  };
  const saveProfile = async (e) => {
    e.preventDefault();
    try {
      const result = await updateCustomerProfile(profile);
      setProfile(result);
      setSuccess("Profile updated.");
    } catch (e) {
      setError(e.message);
    }
  };
  const viewInvoice = async (invoice) => {
    setSelectedInvoice(invoice);
    setPaymentState("");
    try {
      setPaymentHistory((await listInvoicePayments(invoice.id)) || []);
    } catch (e) {
      setError(e.message || "Unable to load payment history.");
    }
  };
  const payInvoice = async () => {
    if (!selectedInvoice) return;
    const digits = paymentPhone.replace(/\D/g, "");
    const normalized = digits.startsWith("0") ? `255${digits.slice(1)}` : digits;
    if (!/^255[67]\d{8}$/.test(normalized)) {
      setError("Enter 0682328642, +255682328642, or 255682328642.");
      return;
    }
    setPaying(true);
    setError("");
    try {
      const payment = await initiateInvoicePayment(selectedInvoice.id, {
        paymentMethod,
        phoneNumber: paymentPhone,
      });
      setPaymentState(payment.status);
      setPaymentHistory((await listInvoicePayments(selectedInvoice.id)) || []);
      if (["SUCCESSFUL", "COMPLETED"].includes(payment.status)) {
        setSelectedInvoice((invoice) =>
          invoice ? { ...invoice, paymentStatus: payment.invoicePaymentStatus || "PAID" } : invoice,
        );
        setSuccess("Payment successful. Your invoice has been marked as paid.");
      } else if (["INITIATED", "PENDING", "PROCESSING", "ACTION_REQUIRED", "UNKNOWN"].includes(payment.status)) {
        if (payment.authorizationUrl) {
          sessionStorage.setItem("flw_payment_return_id", String(payment.id));
          sessionStorage.setItem("flw_payment_return_invoice_id", String(selectedInvoice.id));
          window.location.assign(payment.authorizationUrl);
          return;
        }
        setSuccess(payment.authorizationInstruction || "Payment request sent. Approve it on your phone.");
      } else {
        setError(payment.failureReason || "Payment request could not be accepted.");
      }
      await refresh();
    } catch (e) {
      setPaymentState("FAILED");
      setError(e.message || "Payment request could not be submitted.");
    } finally {
      setPaying(false);
    }
  };
  const rows = (items, cols, render) => (
    <div className="fef-panel">
      <div className="fef-table-wrap">
        <table className="fef-table">
          <thead>
            <tr>
              {cols.map((c) => (
                <th key={c}>{c}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {items.map(render)}
            {!items.length && (
              <tr>
                <td colSpan={cols.length} style={{ textAlign: "center", padding: 28 }}>
                  Nothing available yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
  const customerName =
    user?.username ||
    [user?.firstName, user?.lastName].filter(Boolean).join(" ") ||
    profile?.contactPerson ||
    "Customer";
  const activePayment = paymentHistory.find((p) => ["INITIATED", "PENDING", "PROCESSING", "ACTION_REQUIRED", "UNKNOWN"].includes(p.status));
  return (
    <DashboardLayout
      role="CUSTOMER"
      pageTitle={SIDE.find((x) => x.key === tab)?.label || "Customer Portal"}
      sideItems={SIDE}
      activeKey={tab}
      onSelect={setTab}
    >
      <PageHeader
        title={
          tab === "dashboard" ? `Welcome, ${customerName}` : SIDE.find((x) => x.key === tab)?.label
        }
        crumbs={["Customer Portal"]}
      />
      {error && (
        <div className="fef-alert fef-alert-danger">
          <FiAlertCircle /> {error}
        </div>
      )}
      {success && <div className="fef-alert fef-alert-success">{success}</div>}
      {tab === "dashboard" && (
        <>
          <div className="fef-panel" style={{ padding: 20, marginBottom: 20 }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                gap: 16,
                alignItems: "center",
                flexWrap: "wrap",
              }}
            >
              <div>
                <h3 style={{ margin: 0 }}>{profile?.companyName || "Your company"}</h3>
                <p style={{ margin: "6px 0 0" }}>
                  {profile?.customerCode} · {profile?.contactPerson || "Contact details pending"}
                </p>
                <p style={{ margin: "4px 0 0" }}>
                  {profile?.email} {profile?.phone && ` · ${profile.phone}`}
                </p>
              </div>
              <button className="fef-btn fef-btn-primary" onClick={() => setTab("new")}>
                <FiPlusCircle /> Apply / Order Fuel
              </button>
            </div>
          </div>
          <div className="fef-stat-grid">
            <StatCard
              label="Total Orders"
              value={data?.totalOrders ?? "…"}
              icon={FiClipboard}
              tone="primary"
            />
            <StatCard
              label="Active Orders"
              value={data?.activeOrders ?? "…"}
              icon={FiTruck}
              tone="warning"
            />
            <StatCard
              label="Awaiting Payment"
              value={data?.awaitingPayment ?? "…"}
              icon={FiFileText}
              tone="warning"
            />
            <StatCard
              label="In Transit"
              value={data?.inTransit ?? "…"}
              icon={FiTruck}
              tone="primary"
            />
            <StatCard
              label="Delivered"
              value={data?.delivered ?? "…"}
              icon={FiHome}
              tone="success"
            />
          </div>
          <h3 style={{ marginTop: 28 }}>Recent Orders</h3>
          {rows(
            data?.recentOrders || [],
            ["Order", "Fuel", "Quantity", "Amount", "Status", ""],
            (o) => (
              <tr key={o.id}>
                <td>{o.orderNumber}</td>
                <td>{o.productName}</td>
                <td>{money(o.quantity)} L</td>
                <td>{money(o.totalAmount)}</td>
                <td>
                  <span className="fef-badge fef-badge-info">{o.customerStatus}</span>
                </td>
                <td>
                  <button
                    className="fef-btn fef-btn-outline"
                    onClick={() => {
                      detail(o);
                      setTab("detail");
                    }}
                  >
                    View
                  </button>
                </td>
              </tr>
            ),
          )}
        </>
      )}
      {tab === "orders" && (
        <>
          {rows(orders, ["Order", "Fuel", "Quantity", "Total", "Status", ""], (o) => (
            <tr key={o.id}>
              <td>{o.orderNumber}</td>
              <td>{o.productName}</td>
              <td>{money(o.quantity)} L</td>
              <td>{money(o.totalAmount)}</td>
              <td>{o.customerStatus}</td>
              <td>
                <button
                  className="fef-btn fef-btn-outline"
                  onClick={() => {
                    detail(o);
                    setTab("detail");
                  }}
                >
                  Details
                </button>
              </td>
            </tr>
          ))}
        </>
      )}
      {tab === "new" && (
        <form className="fef-panel" onSubmit={submit} style={{ maxWidth: 820, padding: 24 }}>
          <div className="fef-form-grid">
            <label className="fef-label">
              Fuel product
              <select className="fef-input" name="productId" defaultValue="" required>
                <option value="" disabled>
                  Select product
                </option>
                {products
                  .filter((p) => p.status !== "DELETED")
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.productName} ({p.fuelType})
                    </option>
                  ))}
              </select>
            </label>
            <label className="fef-label">
              Quantity (litres)
              <input
                className="fef-input"
                name="quantity"
                type="number"
                min="0.01"
                step="0.01"
                required
              />
            </label>
          </div>
          <div style={{ marginTop: 16 }}>
            <OpenStreetMapLocationPicker
              value={location}
              onChange={selectLocation}
              routePreview={routePreview}
            />
          </div>
          {routeLoading && <p style={{ marginTop: 16 }}>Calculating road distance…</p>}
          {routePreview && (
            <div
              style={{
                marginTop: 16,
                padding: 16,
                background:
                  routePreview.routeType === "ESTIMATED_STRAIGHT_LINE" ? "#fffbeb" : "#f0fdf4",
                borderRadius: 8,
              }}
            >
              <strong>
                {routePreview.routeType === "ESTIMATED_STRAIGHT_LINE"
                  ? "Estimated Distance"
                  : "Road Distance"}
              </strong>
              <br />
              {Number(routePreview.distanceKm).toFixed(1)} km ·{" "}
              {Math.ceil(Number(routePreview.durationSeconds) / 60)} min
              <br />
              <strong>Transport Charge</strong>
              <br />
              TZS {money(routePreview.transportPrice)}
            </div>
          )}
          <label className="fef-label" style={{ display: "block", marginTop: 16 }}>
            Notes
            <textarea className="fef-input" name="notes" />
          </label>
          <button
            className="fef-btn fef-btn-primary"
            style={{ marginTop: 20 }}
            disabled={routeLoading || !routePreview}
          >
            Submit for Sales Review
          </button>
        </form>
      )}
      {tab === "invoices" && (
        <>
          <>
            {rows(invoices, ["Invoice", "Order", "Date", "Amount", "Payment", ""], (i) => (
              <tr key={i.id}>
                <td>{i.invoiceNumber}</td>
                <td>{i.orderNumber}</td>
                <td>{i.invoiceDate?.slice(0, 10)}</td>
                <td>{money(i.grandTotal)}</td>
                <td>{i.paymentDisplayStatus || (i.paymentStatus === "PAID" ? "Paid" : "Unpaid")}</td>
                <td style={{ display: "flex", gap: 8 }}>
                  <button className="fef-btn fef-btn-outline" onClick={() => viewInvoice(i)}>
                    {i.paymentStatus === "PAID" ? "Details" : "Pay Invoice"}
                  </button>
                  <button
                    className="fef-btn fef-btn-outline"
                    onClick={() =>
                      download(`/customer-portal/invoices/${i.id}/pdf`, `${i.invoiceNumber}.pdf`)
                    }
                  >
                    <FiDownload /> PDF
                  </button>
                </td>
              </tr>
            ))}
          </>
          {selectedInvoice && (
            <div className="fef-panel" style={{ padding: 24, marginTop: 20, maxWidth: 760 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                <h3 style={{ marginTop: 0 }}>Invoice {selectedInvoice.invoiceNumber}</h3>
                <button
                  className="fef-btn fef-btn-outline"
                  onClick={() => setSelectedInvoice(null)}
                >
                  Close
                </button>
              </div>
              <div className="fef-form-grid">
                <p>
                  <strong>Fuel Product</strong>
                  <br />
                  {selectedInvoice.productName}
                </p>
                <p>
                  <strong>Quantity</strong>
                  <br />
                  {money(selectedInvoice.quantity)} L
                </p>
                <p>
                  <strong>Total Amount</strong>
                  <br />
                  {money(selectedInvoice.grandTotal)}
                </p>
                <p>
                  <strong>Payment Status</strong>
                  <br />
                  {selectedInvoice.paymentDisplayStatus || (selectedInvoice.paymentStatus === "PAID" ? "Paid" : "Unpaid")}
                </p>
              </div>
              {selectedInvoice.paymentStatus !== "PAID" && !activePayment ? (
                <div style={{ marginTop: 16, padding: 16, background: "#f8fafc", borderRadius: 8 }}>
                  <h4 style={{ marginTop: 0 }}>Pay by mobile money</h4>
                  <p>
                    Invoice: <strong>{selectedInvoice.invoiceNumber}</strong>
                    <br />
                    Amount: <strong>TZS {money(selectedInvoice.grandTotal)}</strong>
                  </p>
                  <div className="fef-form-grid">
                    <label className="fef-label">
                      Provider
                      <select
                        className="fef-input"
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                      >
                        <option value="AIRTEL_MONEY">Airtel Money</option>
                        <option value="MIXX_BY_YAS">Mixx by Yas</option>
                        <option value="HALOPESA">HaloPesa</option>
                        <option value="VODACOM_MONEY">M-Pesa / Vodacom</option>
                      </select>
                    </label>
                    <label className="fef-label">
                      Tanzanian mobile number
                      <input
                        className="fef-input"
                        type="tel"
                        inputMode="numeric"
                        maxLength="13"
                        placeholder="0787835248, 255787835248, or +255787835248"
                        value={paymentPhone}
                        onChange={(e) => setPaymentPhone(e.target.value.replace(/[^0-9+]/g, ""))}
                        required
                      />
                      <small style={{ display: "block", marginTop: 6, color: "var(--feftms-text-muted)" }}>
                        Enter the number exactly as you have it. The system accepts local 0-prefix and +255/255 formats.
                      </small>
                    </label>
                  </div>
                  <button
                    className="fef-btn fef-btn-primary"
                    style={{ marginTop: 16 }}
                    onClick={payInvoice}
                    disabled={paying}
                  >
                    {paying ? "Submitting payment…" : "Pay now"}
                  </button>
                </div>
              ) : selectedInvoice.paymentStatus === "PAID" ? (
                <p style={{ marginTop: 16, color: "var(--feftms-success)", fontWeight: 700 }}>
                  PAID
                </p>
              ) : (
                <div style={{ marginTop: 16, padding: 16, background: "#fff7ed", borderRadius: 8 }}>
                  <strong>{paymentLabel(activePayment?.status)}</strong>
                  <p style={{ margin: "8px 0 0" }}>{activePayment?.authorizationInstruction || "Payment request sent. Approve it on your phone, then use Check Status below."}</p>
                  <p style={{ margin: "8px 0 0", fontSize: 13 }}>Sandbox note: this is a simulated Flutterwave request; it does not send a real Airtel Money prompt or charge the displayed number.</p>
                </div>
              )}
              {paymentState && (
                <p style={{ marginTop: 12 }}>
                  <strong>{paymentLabel(paymentState)}</strong>
                </p>
              )}
              {paymentHistory.length > 0 && (
                <>
                  <h4 style={{ marginTop: 24 }}>Payment History</h4>
                  {rows(
                    paymentHistory,
                    ["Reference", "Method", "Amount", "Status", "Provider", "Date", "Details", ""],
                    (p) => (
                      <tr key={p.id}>
                        <td>{p.paymentReference}</td>
                        <td>{p.paymentMethod}{p.mobileMoneyNetwork ? ` · ${p.mobileMoneyNetwork}` : ""}<br /><small>{p.maskedPhoneNumber || ""}</small></td>
                        <td>
                          {p.currency} {money(p.amount)}
                        </td>
                        <td>{paymentLabel(p.status)}</td>
                        <td>{p.providerReference || "—"}</td>
                        <td>{p.completedAt?.slice(0, 10) || p.initiatedAt?.slice(0, 10)}</td>
                        <td title={p.failureReason || ""}>{p.failureReason || "—"}</td>
                        <td style={{ display: "flex", gap: 6 }}>
                          {["INITIATED", "PENDING", "PROCESSING", "ACTION_REQUIRED", "UNKNOWN"].includes(p.status) && <button className="fef-btn fef-btn-outline" onClick={async () => { setPaying(true); try { await refreshCustomerPayment(p.id); setPaymentHistory((await listInvoicePayments(selectedInvoice.id)) || []); await refresh(); } finally { setPaying(false); } }} disabled={paying}>Check Status</button>}
                          {["INITIATED", "PENDING", "PROCESSING", "ACTION_REQUIRED", "UNKNOWN"].includes(p.status) && <button className="fef-btn fef-btn-outline" onClick={async () => { setPaying(true); try { await cancelCustomerPayment(p.id); setPaymentHistory((await listInvoicePayments(selectedInvoice.id)) || []); await refresh(); setPaymentState("CANCELLED"); } finally { setPaying(false); } }} disabled={paying}>Cancel</button>}
                        </td>
                      </tr>
                    ),
                  )}
                </>
              )}
            </div>
          )}
        </>
      )}
      {tab === "deliveries" && (
        <div className="fef-panel">
          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Delivery</th>
                  <th>Order</th>
                  <th>Truck</th>
                  <th>Destination</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {deliveries.map((d) => (
                  <Fragment key={d.id}>
                    <tr key={d.id}>
                      <td>{d.deliveryNumber}</td>
                      <td>{d.orderNumber}</td>
                      <td>{d.truckNumber || "Assigned"}</td>
                      <td>{d.destination}</td>
                      <td>{d.deliveryStatus}</td>
                      <td>{d.dispatchedAt?.slice(0, 10) || "—"}</td>
                      <td style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                        <button
                          className="fef-btn fef-btn-outline"
                          onClick={() => toggleDeliveryDocuments(d)}
                          disabled={!d.orderId}
                        >
                          {expandedDeliveryId === d.id ? "Hide documents" : "View documents"}
                        </button>
                        <button
                          className="fef-btn fef-btn-primary"
                          onClick={() => toggleLiveTracking(d)}
                          disabled={d.deliveryStatus !== "IN_TRANSIT"}
                          title={d.deliveryStatus === "IN_TRANSIT" ? "View the driver's current location" : "Live tracking is available while the delivery is in transit"}
                        >
                          <FiMapPin /> {trackingDeliveryId === d.id ? "Hide live map" : "Track delivery"}
                        </button>
                      </td>
                    </tr>
                    {trackingDeliveryId === d.id && (
                      <tr key={`${d.id}-tracking`}>
                        <td colSpan="7" style={{ padding: 20, background: "#f8fafc" }}>
                          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", marginBottom: 12 }}>
                            <div>
                              <strong>Live driver location</strong>
                              <div style={{ color: "var(--feftms-text-muted)", fontSize: 13, marginTop: 4 }}>Refreshes automatically every 20 seconds while this delivery is in transit.</div>
                            </div>
                            {tracking?.updatedAt && <span className="fef-badge fef-badge-info">Last update: {new Date(tracking.updatedAt).toLocaleTimeString()}</span>}
                          </div>
                          {trackingError ? <p className="fef-alert fef-alert-danger">{trackingError}</p> : !tracking ? <p>Loading live location…</p> : tracking.live ? <><DeliveryTrackingMap latitude={tracking.latitude} longitude={tracking.longitude} /><p style={{ margin: "12px 0 0" }}><strong>Current area:</strong> {trackingPlace || "Looking up current area…"}<br /><small>{Number(tracking.latitude).toFixed(6)}, {Number(tracking.longitude).toFixed(6)}{tracking.accuracy != null ? ` · accuracy about ${Math.round(tracking.accuracy)} m` : ""}</small></p></> : <p>The driver has not shared a live location yet. Tracking will appear after the driver starts the trip and allows location access.</p>}
                        </td>
                      </tr>
                    )}
                    {expandedDeliveryId === d.id && (
                      <tr key={`${d.id}-documents`}>
                        <td colSpan="7" style={{ padding: 20, background: "#f8fafc" }}>
                          <strong>Delivery documents</strong>
                          {loadingDeliveryDocuments === d.id ? (
                            <p style={{ margin: "8px 0 0" }}>Loading documents…</p>
                          ) : (
                            <div
                              style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 10 }}
                            >
                              {(deliveryDocuments[d.id] || []).map((doc) => (
                                <button
                                  key={`${doc.type}-${doc.id}`}
                                  className="fef-btn fef-btn-outline"
                                  onClick={() => download(doc.endpoint, `${doc.number}.pdf`)}
                                >
                                  <FiDownload /> {doc.type}: {doc.number}
                                </button>
                              ))}
                              {!(deliveryDocuments[d.id] || []).length && (
                                <span>No documents are available for this delivery yet.</span>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
                {!deliveries.length && (
                  <tr>
                    <td colSpan="7" style={{ textAlign: "center", padding: 28 }}>
                      Nothing available yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
      {tab === "profile" && profile && (
        <form className="fef-panel" onSubmit={saveProfile} style={{ maxWidth: 720, padding: 24 }}>
          <p>
            <strong>{profile.companyName}</strong> · {profile.customerCode}
          </p>
          <div className="fef-form-grid">
            <label className="fef-label">
              Contact person
              <input
                className="fef-input"
                value={profile.contactPerson || ""}
                onChange={(e) => setProfile({ ...profile, contactPerson: e.target.value })}
              />
            </label>
            <label className="fef-label">
              Email
              <input
                className="fef-input"
                type="email"
                value={profile.email || ""}
                onChange={(e) => setProfile({ ...profile, email: e.target.value })}
              />
            </label>
            <label className="fef-label">
              Phone
              <input
                className="fef-input"
                value={profile.phone || ""}
                onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
              />
            </label>
            <label className="fef-label">
              Address
              <textarea
                className="fef-input"
                value={profile.address || ""}
                onChange={(e) => setProfile({ ...profile, address: e.target.value })}
              />
            </label>
          </div>
          <button className="fef-btn fef-btn-primary" style={{ marginTop: 20 }}>
            Save profile
          </button>
        </form>
      )}
      {tab === "detail" && selected && (
        <div className="fef-panel" style={{ padding: 24 }}>
          <button className="fef-btn fef-btn-outline" onClick={() => setTab("orders")}>
            Back to Orders
          </button>
          <h2>{selected.orderNumber}</h2>
          <p>
            {selected.productName} · {money(selected.quantity)} L · {selected.customerStatus}
          </p>
          <h3>Order progress</h3>
          {timeline?.steps?.map((s) => (
            <div
              key={s.key}
              style={{
                padding: "8px 0",
                color: s.current
                  ? "var(--feftms-primary)"
                  : s.complete
                    ? "var(--feftms-success)"
                    : "var(--feftms-text-muted)",
                fontWeight: s.current ? 700 : 400,
              }}
            >
              {s.complete ? "✓" : s.current ? "●" : "○"} {s.label}
            </div>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
