import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import {
  FiFileText,
  FiPrinter,
  FiUserCheck,
  FiCheckCircle,
  FiAlertCircle,
  FiHome,
  FiArrowLeft,
  FiInfo,
  FiX,
  FiDollarSign,
  FiTruck,
  FiUser,
  FiCompass,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { OperatorWorkflowProgress } from "../components/OperatorWorkflowProgress";
import { listLoadingOrders } from "../services/loadingOrderService";
import {
  generateDeliveryNote,
  generateTruckInvoice,
  getDeliveryNote,
  getTruckInvoice,
  printDeliveryNote,
  printTruckInvoice,
  markHandedToDriver,
} from "../services/deliveryDocumentService";
import "../styles/forms.css";

export const Route = createFileRoute("/delivery-documents")({
  head: () => ({ meta: [{ title: "Documentation Workspace — FEFTMS" }] }),
  component: DeliveryDocumentsWorkspace,
});

const SIDE = [
  { key: "operations", label: "Operations", icon: FiArrowLeft },
  { key: "docs", label: "Delivery Documents", icon: FiFileText },
  { key: "dispatch", label: "Dispatch Management", icon: FiTruck },
  { key: "deliveries", label: "Delivery Management", icon: FiCompass },
];

function DeliveryDocumentsWorkspace() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("docs");
  const [activities, setActivities] = useState([]);
  const [docStatusMap, setDocStatusMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Preview modals
  const [previewNote, setPreviewNote] = useState(null);
  const [previewInvoice, setPreviewInvoice] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const orders = await listLoadingOrders();
      const allActivities = [];
      orders.forEach((order) => {
        if (order.activities) {
          order.activities.forEach((activity) => {
            if (activity.status === "COMPLETED") {
              allActivities.push({
                ...activity,
                loadingOrderNumber: order.loadingOrderNumber,
                loadingOrderId: order.id,
                loadingTerminal: order.loadingTerminal,
                consignee: order.consignee,
              });
            }
          });
        }
      });
      setActivities(allActivities);
      await fetchDocStatuses(allActivities);
    } catch (err) {
      console.error(err);
      setError("Failed to load loading activities and document statuses.");
    } finally {
      setLoading(false);
    }
  };

  const fetchDocStatuses = async (completedActivities) => {
    const statuses = {};
    await Promise.all(
      completedActivities.map(async (act) => {
        statuses[act.id] = { dn: null, inv: null, loading: true };
        try {
          const dnRes = await getDeliveryNote(act.id);
          statuses[act.id].dn = dnRes || null;
        } catch (e) {
          // not generated
        }
        try {
          const invRes = await getTruckInvoice(act.id);
          statuses[act.id].inv = invRes || null;
        } catch (e) {
          // not generated
        }
        statuses[act.id].loading = false;
      })
    );
    setDocStatusMap(statuses);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleTabSelect = (key) => {
    if (key === "operations") {
      navigate({ to: "/operations" });
    } else if (key === "dispatch") {
      navigate({ to: "/dispatch" });
    } else if (key === "deliveries") {
      navigate({ to: "/deliveries" });
    } else {
      setActiveTab(key);
    }
  };

  const handleGenerateDN = async (activityId) => {
    setError("");
    setSuccess("");
    try {
      const res = await generateDeliveryNote(activityId);
      setSuccess(`Delivery Note ${res.deliveryNoteNumber} generated successfully!`);
      await loadData();
    } catch (err) {
      setError(err?.message || "Failed to generate Delivery Note.");
    }
  };

  const handleGenerateInv = async (activityId) => {
    setError("");
    setSuccess("");
    try {
      const res = await generateTruckInvoice(activityId);
      setSuccess(`Truck Invoice ${res.invoiceNumber} generated successfully!`);
      await loadData();
    } catch (err) {
      setError(err?.message || "Failed to generate Truck Invoice.");
    }
  };

  const handlePrintDN = async (noteId) => {
    try {
      const res = await printDeliveryNote(noteId);
      setPreviewNote(res);
      // Trigger browser print for printable section
      setTimeout(() => {
        window.print();
      }, 500);
      await loadData();
    } catch (err) {
      setError(err?.message || "Failed to mark Delivery Note as printed.");
    }
  };

  const handlePrintInv = async (invoiceId) => {
    try {
      const res = await printTruckInvoice(invoiceId);
      setPreviewInvoice(res);
      // Trigger browser print for printable section
      setTimeout(() => {
        window.print();
      }, 500);
      await loadData();
    } catch (err) {
      setError(err?.message || "Failed to mark Truck Invoice as printed.");
    }
  };

  const handleHandover = async (noteId) => {
    setError("");
    setSuccess("");
    try {
      const res = await markHandedToDriver(noteId);
      setSuccess(`Delivery Note ${res.deliveryNoteNumber} successfully handed to driver!`);
      await loadData();
    } catch (err) {
      setError(err?.message || "Failed to record handover to driver.");
    }
  };

  const pendingDocsCount = activities.filter(
    (a) => !docStatusMap[a.id]?.dn || !docStatusMap[a.id]?.inv
  ).length;

  const readyDocsCount = activities.filter(
    (a) => docStatusMap[a.id]?.dn && docStatusMap[a.id]?.inv
  ).length;

  return (
    <RouteGuard allowedRoles={["OPERATIONS", "ADMIN", "OPERATOR"]}>
      <DashboardLayout
        role="OPERATIONS"
        sideItems={SIDE}
        activeKey={activeTab}
        onSelect={handleTabSelect}
      >
        <PageHeader title="Delivery Documents" crumbs={["Operations", "Delivery Documents"]} />
        <OperatorWorkflowProgress current="Documentation" nextLabel="Dispatch Management" onNext={() => navigate({ to: "/dispatch" })} />

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

        <div className="fef-stat-grid">
          <StatCard
            label="Total Completed Loadings"
            value={loading ? "…" : String(activities.length)}
            icon={FiTruck}
            tone="primary"
          />
          <StatCard
            label="Awaiting Documentation"
            value={loading ? "…" : String(pendingDocsCount)}
            icon={FiFileText}
            tone="warning"
          />
          <StatCard
            label="Documents Ready"
            value={loading ? "…" : String(readyDocsCount)}
            icon={FiCheckCircle}
            tone="success"
          />
        </div>

        <div className="fef-panel" style={{ marginTop: 24 }}>
          <div className="fef-panel-head">
            <h3>Truck Loading & Operations Documentation Queue</h3>
          </div>
          <div className="fef-table-wrap">
            <table className="fef-table">
              <thead>
                <tr>
                  <th>Truck / Order</th>
                  <th>Driver & Carrier</th>
                  <th>Fuel Details</th>
                  <th>Loading Report</th>
                  <th>Delivery Note Status</th>
                  <th>Truck Invoice Status</th>
                  <th>Documentation Actions</th>
                </tr>
              </thead>
              <tbody>
                {activities.map((a) => {
                  const state = docStatusMap[a.id] || { dn: null, inv: null, loading: true };
                  const reportExist = a.reports && a.reports.length > 0;
                  const reportNumber = reportExist ? a.reports[0].reportNumber : null;

                  return (
                    <tr key={a.id}>
                      <td>
                        <strong>{a.truckNumber}</strong>
                        <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                          LO: {a.loadingOrderNumber}
                        </div>
                      </td>
                      <td>
                        <div>{a.driverName}</div>
                        <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                          {a.transportCompany}
                        </div>
                      </td>
                      <td>
                        <div>{a.product}</div>
                        <div style={{ fontSize: 11, color: "var(--feftms-text-muted)" }}>
                          Std Vol: {a.standardVolume?.toLocaleString()} L
                        </div>
                      </td>
                      <td>
                        {reportExist ? (
                          <span className="fef-badge fef-badge-success" title={`Report: ${reportNumber}`}>
                            GENERATED
                          </span>
                        ) : (
                          <span className="fef-badge fef-badge-danger">MISSING</span>
                        )}
                      </td>
                      <td>
                        {state.loading ? (
                          <span style={{ fontSize: 12, color: "var(--feftms-text-muted)" }}>Loading…</span>
                        ) : state.dn ? (
                          <span className={`fef-badge fef-badge-${state.dn.status === "HANDED_TO_DRIVER" ? "success" : state.dn.status === "PRINTED" ? "info" : "warning"}`}>
                            {state.dn.status}
                          </span>
                        ) : (
                          <span className="fef-badge fef-badge-pending">NOT GENERATED</span>
                        )}
                      </td>
                      <td>
                        {state.loading ? (
                          <span style={{ fontSize: 12, color: "var(--feftms-text-muted)" }}>Loading…</span>
                        ) : state.inv ? (
                          <span className={`fef-badge fef-badge-${state.inv.invoiceStatus === "PRINTED" ? "success" : "info"}`}>
                            {state.inv.invoiceStatus}
                          </span>
                        ) : (
                          <span className="fef-badge fef-badge-pending">NOT GENERATED</span>
                        )}
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                          {/* Delivery Note Action */}
                          {!state.dn && reportExist && (
                            <button
                              className="fef-btn fef-btn-primary"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => handleGenerateDN(a.id)}
                            >
                              Generate DN
                            </button>
                          )}
                          {state.dn && (
                            <>
                              <button
                                className="fef-btn fef-btn-outline"
                                style={{ padding: "4px 8px", fontSize: 11 }}
                                onClick={() => setPreviewNote(state.dn)}
                              >
                                Preview DN
                              </button>
                              {state.dn.status === "PREPARED" && (
                                <button
                                  className="fef-btn fef-btn-outline"
                                  style={{ padding: "4px 8px", fontSize: 11 }}
                                  onClick={() => handlePrintDN(state.dn.id)}
                                >
                                  <FiPrinter style={{ marginRight: 4 }} /> Print DN
                                </button>
                              )}
                            </>
                          )}

                          {/* Truck Invoice Action */}
                          {state.dn && !state.inv && (
                            <button
                              className="fef-btn fef-btn-primary"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => handleGenerateInv(a.id)}
                            >
                              Generate Invoice
                            </button>
                          )}
                          {state.inv && (
                            <>
                              <button
                                className="fef-btn fef-btn-outline"
                                style={{ padding: "4px 8px", fontSize: 11 }}
                                onClick={() => setPreviewInvoice(state.inv)}
                              >
                                Preview INV
                              </button>
                              {state.inv.invoiceStatus === "GENERATED" && (
                                <button
                                  className="fef-btn fef-btn-outline"
                                  style={{ padding: "4px 8px", fontSize: 11 }}
                                  onClick={() => handlePrintInv(state.inv.id)}
                                >
                                  <FiPrinter style={{ marginRight: 4 }} /> Print INV
                                </button>
                              )}
                            </>
                          )}

                          {/* Handover Action */}
                          {state.dn && state.dn.status === "PRINTED" && (
                            <button
                              className="fef-btn fef-btn-success"
                              style={{ padding: "4px 8px", fontSize: 11 }}
                              onClick={() => handleHandover(state.dn.id)}
                            >
                              <FiUserCheck style={{ marginRight: 4 }} /> Handover to Driver
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {activities.length === 0 && !loading && (
                  <tr>
                    <td colSpan="7" style={{ textAlign: "center", color: "var(--feftms-text-muted)", padding: 30 }}>
                      <FiInfo size={24} style={{ marginBottom: 8, opacity: 0.5 }} />
                      <p>No loading activities are ready for documentation. Complete loading and generate its Loading Report in Operations first.</p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* DELIVERY NOTE PREVIEW MODAL */}
        {previewNote &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setPreviewNote(null)}>
              <div className="fef-modal-window" style={{ maxWidth: "800px" }} onClick={(e) => e.stopPropagation()}>
                <button className="fef-modal-close" onClick={() => setPreviewNote(null)}>
                  <FiX />
                </button>
                <div className="fef-print-area" id="print-dn-section">
                  <div style={{ display: "flex", justifyContent: "between", alignItems: "center", borderBottom: "2px solid #E5E7EB", paddingBottom: 15 }}>
                    <div>
                      <h1 style={{ color: "#F97316", margin: 0, fontWeight: 700, letterSpacing: "1px" }}>FALCON ENERGY</h1>
                      <small style={{ color: "var(--feftms-text-muted)" }}>Fuel Transportation Management System</small>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <h3 style={{ margin: 0, color: "var(--feftms-text-normal)" }}>DELIVERY NOTE</h3>
                      <span style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>#{previewNote.deliveryNoteNumber}</span>
                    </div>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginTop: 20 }}>
                    <div>
                      <h4 style={{ margin: "0 0 8px 0", color: "#F97316" }}>Customer Information</h4>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Customer:</strong> {previewNote.customerName}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Destination:</strong> {previewNote.destination}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Terminal:</strong> {previewNote.loadingTerminal || "Main Terminal"}</p>
                    </div>
                    <div>
                      <h4 style={{ margin: "0 0 8px 0", color: "#F97316" }}>Document Reference</h4>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Date Prepared:</strong> {previewNote.preparedAt ? new Date(previewNote.preparedAt).toLocaleString() : "TBA"}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Prepared By:</strong> {previewNote.preparedBy}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Loading Order:</strong> LO-{previewNote.loadingOrderId}</p>
                    </div>
                  </div>

                  <div style={{ marginTop: 20, border: "1px solid #E5E7EB", borderRadius: 6, overflow: "hidden" }}>
                    <div style={{ background: "rgba(255,255,255,0.05)", padding: "10px 15px", fontWeight: "bold", fontSize: 13, display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1.5fr" }}>
                      <span>Truck & Driver Details</span>
                      <span>License Number</span>
                      <span>Carrier</span>
                      <span>Status</span>
                    </div>
                    <div style={{ padding: "12px 15px", fontSize: 13, display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1.5fr" }}>
                      <span>{previewNote.truckNumber} ({previewNote.driverName})</span>
                      <span>{previewNote.driverLicenseNumber}</span>
                      <span>{previewNote.transportCompany}</span>
                      <span><span className="fef-badge fef-badge-success">{previewNote.status}</span></span>
                    </div>
                  </div>

                  <div style={{ marginTop: 20, border: "1px solid #E5E7EB", borderRadius: 6, overflow: "hidden" }}>
                    <div style={{ background: "rgba(255,255,255,0.05)", padding: "10px 15px", fontWeight: "bold", fontSize: 13, display: "grid", gridTemplateColumns: "2fr 1fr 1fr" }}>
                      <span>Fuel Product</span>
                      <span>Ambient Volume</span>
                      <span>Standard Volume</span>
                    </div>
                    <div style={{ padding: "12px 15px", fontSize: 13, display: "grid", gridTemplateColumns: "2fr 1fr 1fr" }}>
                      <span>{previewNote.productName}</span>
                      <span>{previewNote.ambientVolume?.toLocaleString()} L</span>
                      <span>{previewNote.standardVolume?.toLocaleString()} L</span>
                    </div>
                  </div>

                  <div style={{ marginTop: 40, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 40, textAlign: "center" }}>
                    <div style={{ borderTop: "1px dashed #9CA3AF", paddingTop: 10 }}>
                      <p style={{ margin: 0, fontSize: 12 }}>Authorized Signature (Operations)</p>
                      <small style={{ color: "var(--feftms-text-muted)" }}>{previewNote.preparedBy}</small>
                    </div>
                    <div style={{ borderTop: "1px dashed #9CA3AF", paddingTop: 10 }}>
                      <p style={{ margin: 0, fontSize: 12 }}>Driver Acknowledgement</p>
                      <small style={{ color: "var(--feftms-text-muted)" }}>{previewNote.driverName}</small>
                    </div>
                  </div>
                </div>

                <div style={{ display: "flex", justifyContent: "end", gap: 10, marginTop: 24, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  {previewNote.status === "PREPARED" && (
                    <button className="fef-btn fef-btn-primary" onClick={() => handlePrintDN(previewNote.id)}>
                      <FiPrinter style={{ marginRight: 6 }} /> Print Delivery Note
                    </button>
                  )}
                  <button className="fef-btn fef-btn-outline" onClick={() => setPreviewNote(null)}>
                    Close
                  </button>
                </div>
              </div>
            </div>,
            document.body
          )}

        {/* TRUCK INVOICE PREVIEW MODAL */}
        {previewInvoice &&
          typeof window !== "undefined" &&
          createPortal(
            <div className="fef-modal-backdrop" onClick={() => setPreviewInvoice(null)}>
              <div className="fef-modal-window" style={{ maxWidth: "800px" }} onClick={(e) => e.stopPropagation()}>
                <button className="fef-modal-close" onClick={() => setPreviewInvoice(null)}>
                  <FiX />
                </button>
                <div className="fef-print-area" id="print-invoice-section">
                  <div style={{ display: "flex", justifyContent: "between", alignItems: "center", borderBottom: "2px solid #E5E7EB", paddingBottom: 15 }}>
                    <div>
                      <h1 style={{ color: "#F97316", margin: 0, fontWeight: 700, letterSpacing: "1px" }}>FALCON ENERGY</h1>
                      <small style={{ color: "var(--feftms-text-muted)" }}>Fuel Transportation Management System</small>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <h3 style={{ margin: 0, color: "var(--feftms-text-normal)" }}>TRUCK INVOICE</h3>
                      <span style={{ fontSize: 13, color: "var(--feftms-text-muted)" }}>#{previewInvoice.invoiceNumber}</span>
                    </div>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginTop: 20 }}>
                    <div>
                      <h4 style={{ margin: "0 0 8px 0", color: "#F97316" }}>Billed To</h4>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Customer:</strong> {previewInvoice.customerName}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Truck Number:</strong> {previewInvoice.truckNumber}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Driver:</strong> {previewInvoice.driverName}</p>
                    </div>
                    <div>
                      <h4 style={{ margin: "0 0 8px 0", color: "#F97316" }}>Invoice Metadata</h4>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Date Generated:</strong> {previewInvoice.createdAt ? new Date(previewInvoice.createdAt).toLocaleString() : "TBA"}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Delivery Note Ref:</strong> {previewInvoice.deliveryNoteNumber}</p>
                      <p style={{ margin: "2px 0", fontSize: 13 }}><strong>Payment Status:</strong> <span style={{ color: "#10B981", fontWeight: "bold" }}>{previewInvoice.paymentStatus}</span></p>
                    </div>
                  </div>

                  <div style={{ marginTop: 20, border: "1px solid #E5E7EB", borderRadius: 6, overflow: "hidden" }}>
                    <div style={{ background: "rgba(255,255,255,0.05)", padding: "10px 15px", fontWeight: "bold", fontSize: 13, display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1fr" }}>
                      <span>Fuel Description</span>
                      <span>Quantity</span>
                      <span>Unit Price</span>
                      <span style={{ textAlign: "right" }}>Total Amount</span>
                    </div>
                    <div style={{ padding: "12px 15px", fontSize: 13, display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1fr" }}>
                      <span>{previewInvoice.productName}</span>
                      <span>{previewInvoice.quantity?.toLocaleString()} L</span>
                      <span>${previewInvoice.unitPrice?.toFixed(2)} / L</span>
                      <span style={{ textAlign: "right", fontWeight: "bold" }}>${previewInvoice.totalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                  </div>

                  <div style={{ display: "flex", justifyContent: "end", marginTop: 20, paddingRight: 15 }}>
                    <div style={{ textAlign: "right", fontSize: 14 }}>
                      <span style={{ color: "var(--feftms-text-muted)" }}>Total Paid: </span>
                      <strong style={{ fontSize: 18, color: "#10B981" }}>${previewInvoice.totalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USD</strong>
                    </div>
                  </div>

                  <div style={{ marginTop: 50, borderTop: "1px solid #E5E7EB", paddingTop: 15, textAlign: "center" }}>
                    <p style={{ margin: 0, fontSize: 12, color: "var(--feftms-text-muted)" }}>Thank you for doing business with Falcon Energy.</p>
                  </div>
                </div>

                <div style={{ display: "flex", justifyContent: "end", gap: 10, marginTop: 24, borderTop: "1px solid #E5E7EB", paddingTop: 15 }}>
                  {previewInvoice.invoiceStatus === "GENERATED" && (
                    <button className="fef-btn fef-btn-primary" onClick={() => handlePrintInv(previewInvoice.id)}>
                      <FiPrinter style={{ marginRight: 6 }} /> Print Invoice
                    </button>
                  )}
                  <button className="fef-btn fef-btn-outline" onClick={() => setPreviewInvoice(null)}>
                    Close
                  </button>
                </div>
              </div>
            </div>,
            document.body
          )}
      </DashboardLayout>
    </RouteGuard>
  );
}
