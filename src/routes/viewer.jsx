import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { FiHome, FiTrendingUp, FiEye, FiSettings } from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { listAuditLogs } from "../services/auditService";
import { listTransactions } from "../services/inventoryService";

export const Route = createFileRoute("/viewer")({
  head: () => ({ meta: [{ title: "Viewer Workspace — FEFTMS" }] }),
  component: ViewerDash,
});

const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "reports", label: "Reports & Logs", icon: FiTrendingUp },
  { key: "settings", label: "Settings", icon: FiSettings },
];

function ViewerDash() {
  const [activeTab, setActiveTab] = useState("dash");
  const [audits, setAudits] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      listAuditLogs({ page: 0, size: 10 }),
      listTransactions({ page: 0, size: 10 }),
    ])
      .then((results) => {
        if (results[0].status === "fulfilled")
          setAudits(results[0].value.content || results[0].value || []);
        if (results[1].status === "fulfilled")
          setTransactions(results[1].value.content || results[1].value || []);
        const failures = results.filter((r) => r.status === "rejected");
        if (failures.length) console.warn("Viewer partial load failures:", failures);
      })
      .catch((e) => console.error("Failed loading viewer data", e))
      .finally(() => setLoading(false));
  }, []);

  const totalHandled = transactions.reduce((acc, t) => acc + (t.quantity || 0), 0);

  return (
    <RouteGuard allowedRoles={["VIEWER"]}>
      <DashboardLayout role="VIEWER" sideItems={SIDE} activeKey={activeTab} onSelect={setActiveTab}>
        <PageHeader title="Viewer Workspace" crumbs={["Viewer", activeTab]} />
        <div className="fef-stat-grid">
          <StatCard
            label="Recent Transactions"
            value={loading ? "…" : String(transactions.length)}
            icon={FiTrendingUp}
            tone="primary"
          />
          <StatCard label="System Health" value="Healthy" icon={FiEye} tone="success" />
        </div>
        <div className="fef-panel" style={{ marginTop: 24 }}>
          <div className="fef-panel-head">
            <h3>Recent Audit Trails</h3>
          </div>
          <div style={{ padding: 20, color: "var(--feftms-text-muted)" }}>
            {loading ? (
              <p>Loading audit trails and transactions…</p>
            ) : (
              <div>
                <h4>Audit Logs</h4>
                <ul>
                  {audits.map((a) => (
                    <li key={a.id}>
                      {a.action} — {a.entity} — {a.createdAt}
                    </li>
                  ))}
                </ul>

                <h4 style={{ marginTop: 12 }}>Recent Transactions</h4>
                <ul>
                  {transactions.map((t) => (
                    <li key={t.id}>
                      {t.transactionNumber || t.id} — {t.transactionType} — {t.quantity}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      </DashboardLayout>
    </RouteGuard>
  );
}
