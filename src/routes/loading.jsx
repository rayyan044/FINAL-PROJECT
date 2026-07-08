import { createFileRoute } from "@tanstack/react-router";
import { FiActivity, FiClipboard, FiCheckCircle, FiDroplet, FiHome } from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { Link } from "@tanstack/react-router";

export const Route = createFileRoute("/loading")({
  head: () => ({ meta: [{ title: "Loading Officer — FEFTMS" }] }),
  component: LoadingDash,
});
const SIDE = [
  { key: "dash", label: "Dashboard", icon: FiHome },
  { key: "today", label: "Today's Loading", icon: FiActivity },
  { key: "reports", label: "Loading Reports", icon: FiClipboard },
  { key: "completed", label: "Completed", icon: FiCheckCircle },
];
function LoadingDash() {
  // The system does not define a dedicated Loading Officer role in the SRS.
  // Disable the separate Loading Dashboard and point users to the Operator/Operations workspace.
  return (
    <DashboardLayout role="OPERATOR" sideItems={SIDE} activeKey="dash">
      <PageHeader title="Loading Operations (Disabled)" crumbs={["Loading"]} />
      <div style={{ padding: 24 }}>
        <p>
          The separate Loading Dashboard is not enabled. Loading functionality is handled from the
          Operator / Operations workspace.
        </p>
        <p>
          Go to the <Link to="/operations">Operations Dashboard</Link> to manage loading bays and operators.
        </p>
      </div>
    </DashboardLayout>
  );
}
