import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  FiFileText,
  FiTrendingUp,
  FiDollarSign,
  FiTruck,
  FiActivity,
  FiDownload,
  FiPrinter,
  FiUsers,
  FiDatabase,
  FiHome,
  FiCalendar,
} from "react-icons/fi";
import { DashboardLayout, PageHeader, StatCard } from "../components/DashboardLayout";
import { RouteGuard } from "../components/RouteGuard";
import { useAuth } from "../context/AuthContext";
import {
  getDashboardSummary,
  getSalesReport,
  getInventoryReport,
  getLoadingReport,
  getDispatchReport,
  getDeliveryReport,
  getCustomerReport,
  getFinancialReport,
  getReportHistory,
} from "../services/reportingService";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { listCustomers } from "../services/customerService";
import { toast } from "sonner";

export const Route = createFileRoute("/reports")({
  head: () => ({ meta: [{ title: "Reports & Analytics — FEFTMS" }] }),
  component: ReportsDash,
});

const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884d8", "#82ca9d"];

export function ReportsDash({ embedded = false, onWorkspaceSelect }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Role resolution
  const userRole = user?.role || "VIEWER";
  const isAdminOrManager = userRole === "ADMIN" || userRole === "MANAGER";
  const isSales = userRole === "SALES_OFFICER" || isAdminOrManager;
  const isFinance = userRole === "FINANCE" || isAdminOrManager;
  const isOperations = userRole === "OPERATIONS" || userRole === "DISPATCHER" || isAdminOrManager;
  const isOperator = userRole === "OPERATOR" || isAdminOrManager;

  // Tabs setup
  const getAvailableTabs = () => {
    const tabs = [];
    if (isAdminOrManager) tabs.push({ id: "overview", label: "Dashboard Overview" });
    if (isSales) tabs.push({ id: "sales", label: "Sales Reports" });
    if (isOperator) tabs.push({ id: "inventory", label: "Inventory Reports" });
    if (isOperations) tabs.push({ id: "operations", label: "Operations Reports" });
    if (isFinance) tabs.push({ id: "finance", label: "Finance Reports" });
    if (isAdminOrManager) tabs.push({ id: "history", label: "Report History" });
    return tabs;
  };

  const tabs = getAvailableTabs();
  const [activeTab, setActiveTab] = useState(tabs[0]?.id || "overview");

  // Date filters
  const [fromDate, setFromDate] = useState(() => {
    const date = new Date();
    date.setDate(date.getDate() - 30);
    return date.toISOString().split("T")[0];
  });
  const [toDate, setToDate] = useState(() => new Date().toISOString().split("T")[0]);

  // Data states
  const [loading, setLoading] = useState(false);
  const [dashboardData, setDashboardData] = useState(null);
  const [salesData, setSalesData] = useState(null);
  const [inventoryData, setInventoryData] = useState(null);
  const [loadingData, setLoadingData] = useState(null);
  const [dispatchData, setDispatchData] = useState(null);
  const [deliveryData, setDeliveryData] = useState(null);
  const [financialData, setFinancialData] = useState(null);
  const [historyData, setHistoryData] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const [customerReportData, setCustomerReportData] = useState(null);

  // Load standard initial data
  useEffect(() => {
    setLoading(true);
    const promises = [];

    if (isAdminOrManager) promises.push(getDashboardSummary().then(res => setDashboardData(res.data)));
    if (isOperator) promises.push(getInventoryReport().then(res => setInventoryData(res.data)));
    if (isSales) {
      promises.push(getSalesReport(fromDate, toDate).then(res => setSalesData(res.data)));
      promises.push(listCustomers().then(res => setCustomers(res.content || res || [])));
    }
    if (isOperations) {
      promises.push(getLoadingReport(fromDate, toDate).then(res => setLoadingData(res.data)));
      promises.push(getDispatchReport(fromDate, toDate).then(res => setDispatchData(res.data)));
      promises.push(getDeliveryReport(fromDate, toDate).then(res => setDeliveryData(res.data)));
    }
    if (isFinance) promises.push(getFinancialReport(fromDate, toDate).then(res => setFinancialData(res.data)));
    if (isAdminOrManager) promises.push(getReportHistory().then(res => setHistoryData(res.data || [])));

    Promise.allSettled(promises)
      .catch(err => console.error("Error fetching reports data:", err))
      .finally(() => setLoading(false));
  }, [userRole]);

  // Filter refresh triggers
  const handleApplyFilters = () => {
    setLoading(true);
    const promises = [];
    if (isSales) promises.push(getSalesReport(fromDate, toDate).then(res => setSalesData(res.data)));
    if (isOperations) {
      promises.push(getLoadingReport(fromDate, toDate).then(res => setLoadingData(res.data)));
      promises.push(getDispatchReport(fromDate, toDate).then(res => setDispatchData(res.data)));
      promises.push(getDeliveryReport(fromDate, toDate).then(res => setDeliveryData(res.data)));
    }
    if (isFinance) promises.push(getFinancialReport(fromDate, toDate).then(res => setFinancialData(res.data)));
    if (isAdminOrManager) promises.push(getReportHistory().then(res => setHistoryData(res.data || [])));

    Promise.allSettled(promises)
      .then(() => toast.success("Filters applied successfully"))
      .catch(err => toast.error("Error applying filters"))
      .finally(() => setLoading(false));
  };

  // Load customer report
  const handleLoadCustomerReport = (id) => {
    if (!id) return;
    setLoading(true);
    getCustomerReport(id)
      .then(res => {
        setCustomerReportData(res.data);
        toast.success("Customer report loaded");
      })
      .catch(err => toast.error("Failed to load customer report"))
      .finally(() => setLoading(false));
  };

  // Export functions (CSV / Printable Excel Simulation)
  const exportToCSV = (data, filename) => {
    if (!data || data.length === 0) {
      toast.warning("No data available to export");
      return;
    }
    const headers = Object.keys(data[0]);
    const csvRows = [];
    csvRows.push(headers.join(","));

    for (const row of data) {
      const values = headers.map(header => {
        const escaped = ("" + row[header]).replace(/"/g, '\\"');
        return `"${escaped}"`;
      });
      csvRows.push(values.join(","));
    }

    const csvContent = "data:text/csv;charset=utf-8," + csvRows.join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `${filename}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const triggerPrint = () => {
    window.print();
  };

  // Sidebar navigation configuration
  const SIDE_ITEMS = [];
  if (userRole === "ADMIN") {
    SIDE_ITEMS.push(
      { key: "dash", label: "Dashboard", icon: FiHome },
      { key: "reports", label: "Reports & Analytics", icon: FiTrendingUp }
    );
  } else {
    SIDE_ITEMS.push(
      { key: "dash", label: "Dashboard Workspace", icon: FiHome },
      { key: "reports", label: "Reports & Analytics", icon: FiTrendingUp }
    );
  }

  const handleSidebarSelect = (key) => {
    if (embedded && onWorkspaceSelect) {
      onWorkspaceSelect(key === "reports" ? "reports" : "dash");
      return;
    }
    if (key === "reports") {
      setActiveTab(tabs[0]?.id || "overview");
    } else {
      const targetRoute = "/" + String(userRole).toLowerCase().replace("_officer", "");
      navigate({ to: targetRoute });
    }
  };

  return (
    <RouteGuard allowedRoles={["ADMIN", "MANAGER", "OPERATIONS", "DISPATCHER", "SALES_OFFICER", "FINANCE", "OPERATOR"]}>
      <DashboardLayout
        embedded={embedded}
        role={userRole}
        userName={user?.username || "User"}
        pageTitle="Reports & Analytics Dashboard"
        sideItems={SIDE_ITEMS}
        activeKey="reports"
        onSelect={handleSidebarSelect}
      >
        {!embedded && <PageHeader title="Reporting & Analytics" crumbs={["Reports", activeTab]} />}

        {/* Global Date & Export Controls */}
        <div className="fef-panel no-print" style={{ marginBottom: 20, padding: 15 }}>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <FiCalendar className="text-primary" size={20} />
              <span className="font-semibold text-sm">Report Interval:</span>
              <input
                type="date"
                className="fef-input py-1 px-2 text-xs border rounded"
                value={fromDate}
                onChange={e => setFromDate(e.target.value)}
              />
              <span className="text-xs">to</span>
              <input
                type="date"
                className="fef-input py-1 px-2 text-xs border rounded"
                value={toDate}
                onChange={e => setToDate(e.target.value)}
              />
              <button onClick={handleApplyFilters} className="fef-btn fef-btn-primary py-1 px-3 text-xs">
                Apply Date Filters
              </button>
            </div>
            <div className="flex gap-2">
              <button onClick={triggerPrint} className="fef-btn fef-btn-secondary flex items-center gap-1 py-1 px-3 text-xs">
                <FiPrinter /> Print / Export PDF
              </button>
            </div>
          </div>
        </div>

        {/* Tabs Bar */}
        <div className="fef-tabs-bar no-print" style={{ display: "flex", gap: 8, marginBottom: 20, borderBottom: "1px solid var(--feftms-border)" }}>
          {tabs.map(t => (
            <button
              key={t.id}
              onClick={() => setActiveTab(t.id)}
              className={`fef-tab-btn py-2 px-4 font-semibold text-sm border-b-2 transition-all ${
                activeTab === t.id
                  ? "border-primary text-primary"
                  : "border-transparent text-muted-foreground hover:text-foreground"
              }`}
              style={{ background: "none" }}
            >
              {t.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex justify-center items-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-primary"></div>
          </div>
        ) : (
          <div className="print-content">
            {/* Overview / Dashboard Summary Tab */}
            {activeTab === "overview" && dashboardData && (
              <div>
                <h2 className="text-xl font-bold mb-4 print-only">Falcon Energy - Operations Overview Dashboard</h2>
                <div className="fef-stat-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: 16, marginBottom: 24 }}>
                  <StatCard label="Total Fuel Orders" value={dashboardData.totalFuelOrders} icon={FiFileText} tone="primary" />
                  <StatCard label="Total Litres Sold" value={`${dashboardData.totalLitresSold?.toLocaleString()} L`} icon={FiTrendingUp} tone="success" />
                  <StatCard label="Total Sales Value" value={`$${dashboardData.totalSalesAmount?.toLocaleString()}`} icon={FiDollarSign} tone="success" />
                  <StatCard label="Current Stock Level" value={`${dashboardData.availableInventory?.toLocaleString()} L`} icon={FiDatabase} tone="warning" />
                  <StatCard label="Trucks Dispatched" value={dashboardData.trucksDispatched} icon={FiTruck} tone="primary" />
                  <StatCard label="Completed Deliveries" value={dashboardData.completedDeliveries} icon={FiActivity} tone="success" />
                  <StatCard label="Total Revenue Collected" value={`$${dashboardData.totalRevenue?.toLocaleString()}`} icon={FiDollarSign} tone="success" />
                  <StatCard label="Outstanding Invoices" value={`$${dashboardData.outstandingInvoices?.toLocaleString()}`} icon={FiFileText} tone="danger" />
                </div>

                <div className="fef-panel" style={{ padding: 20, marginBottom: 24 }}>
                  <h3>Key Performance Dashboard Summary</h3>
                  <div style={{ height: 320, marginTop: 20 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={[
                          { name: "Total Litres Sold", value: dashboardData.totalLitresSold },
                          { name: "Current Available Inventory", value: dashboardData.availableInventory }
                        ]}
                      >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip formatter={(v) => `${v.toLocaleString()} Litres`} />
                        <Legend />
                        <Bar dataKey="value" fill="#0088FE" name="Fuel (Litres)" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            )}

            {/* Sales Tab */}
            {activeTab === "sales" && salesData && (
              <div>
                <h2 className="text-xl font-bold mb-4 print-only">Falcon Energy - Fuel Sales Performance Report</h2>
                <div className="fef-stat-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))", gap: 16, marginBottom: 24 }}>
                  <StatCard label="Orders Created" value={salesData.numberOrders} icon={FiFileText} tone="primary" />
                  <StatCard label="Total Fuel Volume Sold" value={`${salesData.totalQuantitySold?.toLocaleString()} Litres`} icon={FiDatabase} tone="success" />
                  <StatCard label="Gross Sales Value" value={`$${salesData.totalSalesValue?.toLocaleString()}`} icon={FiDollarSign} tone="success" />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: 20, marginBottom: 24 }}>
                  <div className="fef-panel" style={{ padding: 20 }}>
                    <h3>Sales Revenue by Product type</h3>
                    <div style={{ height: 260, marginTop: 20 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={salesData.salesByProduct}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="productName" />
                          <YAxis />
                          <Tooltip formatter={(v) => `$${v.toLocaleString()}`} />
                          <Legend />
                          <Bar dataKey="amount" fill="#00C49F" name="Amount ($)" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>

                  <div className="fef-panel" style={{ padding: 20 }}>
                    <h3>Volume Distribution by Customer</h3>
                    <div style={{ height: 260, marginTop: 20 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={salesData.salesByCustomer}
                            dataKey="volume"
                            nameKey="customerName"
                            cx="50%"
                            cy="50%"
                            outerRadius={80}
                            fill="#8884d8"
                            label={({ customerName, volume }) => `${customerName}: ${volume?.toLocaleString()}L`}
                          >
                            {salesData.salesByCustomer?.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip formatter={(v) => `${v.toLocaleString()} Litres`} />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>

                {/* Customer History Report Section */}
                <div className="fef-panel no-print" style={{ padding: 20, marginBottom: 24 }}>
                  <div className="flex items-center gap-3 mb-4">
                    <FiUsers className="text-primary" size={20} />
                    <h3>Load Customer Purchase History Report</h3>
                    <select
                      className="fef-input max-w-sm"
                      value={selectedCustomerId}
                      onChange={e => {
                        setSelectedCustomerId(e.target.value);
                        handleLoadCustomerReport(e.target.value);
                      }}
                    >
                      <option value="">-- Choose Customer --</option>
                      {customers.map(c => (
                        <option key={c.id} value={c.id}>{c.companyName}</option>
                      ))}
                    </select>
                  </div>

                  {customerReportData && (
                    <div className="fef-panel-body" style={{ background: "var(--feftms-bg-light)", padding: 15, borderRadius: 8 }}>
                      <div className="flex justify-between items-center mb-3">
                        <h4 className="font-bold text-primary">{customerReportData.customerName} Report Summary</h4>
                        <button
                          onClick={() => exportToCSV(customerReportData.customerOrderHistory, `${customerReportData.customerName}_History`)}
                          className="fef-btn fef-btn-secondary py-1 px-3 text-xs flex items-center gap-1"
                        >
                          <FiDownload /> Export CSV
                        </button>
                      </div>
                      <div className="grid grid-cols-3 gap-4 mb-4">
                        <div className="bg-background p-3 rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Purchased Volume</small>
                          <span className="font-bold text-base">{customerReportData.totalPurchasedVolume?.toLocaleString()} L</span>
                        </div>
                        <div className="bg-background p-3 rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Total Amount Paid</small>
                          <span className="font-bold text-base text-success">${customerReportData.totalAmountPaid?.toLocaleString()}</span>
                        </div>
                        <div className="bg-background p-3 rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Delivered Trucks</small>
                          <span className="font-bold text-base">{customerReportData.deliveredTrucks}</span>
                        </div>
                      </div>

                      <table className="fef-table w-full text-xs">
                        <thead>
                          <tr>
                            <th>Order No</th>
                            <th>Order Date</th>
                            <th>Quantity</th>
                            <th>Amount</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {customerReportData.customerOrderHistory?.map(o => (
                            <tr key={o.orderId}>
                              <td className="font-semibold">{o.orderNumber}</td>
                              <td>{new Date(o.orderDate).toLocaleDateString()}</td>
                              <td>{o.quantity?.toLocaleString()} L</td>
                              <td>${o.amount?.toLocaleString()}</td>
                              <td>
                                <span className={`badge ${o.status === "DELIVERED" || o.status === "PAID" ? "badge-success" : "badge-warning"}`}>
                                  {o.status}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Inventory Tab */}
            {activeTab === "inventory" && inventoryData && (
              <div>
                <h2 className="text-xl font-bold mb-4 print-only">Falcon Energy - Inventory Levels & Movements</h2>
                <div className="fef-stat-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 16, marginBottom: 24 }}>
                  <StatCard label="Total Stock Available" value={`${inventoryData.currentStock?.toLocaleString()} L`} icon={FiDatabase} tone="primary" />
                  <StatCard label="Stock Loaded (Deductions)" value={`${inventoryData.loadingDeductions?.toLocaleString()} L`} icon={FiTruck} tone="warning" />
                  <StatCard label="Stock Adjustments" value={`${inventoryData.adjustments?.toLocaleString()} L`} icon={FiTrendingUp} tone="info" />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: 20, marginBottom: 24 }}>
                  <div className="fef-panel" style={{ padding: 20 }}>
                    <h3>Remaining Quantity by Fuel Product</h3>
                    <div style={{ height: 260, marginTop: 20 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={inventoryData.remainingQuantityByProduct}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="productName" />
                          <YAxis />
                          <Tooltip formatter={(v) => `${v.toLocaleString()} L`} />
                          <Legend />
                          <Bar dataKey="openingQuantity" fill="#8884d8" name="Opening Volume" radius={[4, 4, 0, 0]} />
                          <Bar dataKey="loadedQuantity" fill="#FF8042" name="Loaded Volume" radius={[4, 4, 0, 0]} />
                          <Bar dataKey="currentBalance" fill="#00C49F" name="Current Stock" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>

                  <div className="fef-panel" style={{ padding: 20 }}>
                    <div className="flex justify-between items-center mb-4">
                      <h3>Recent Stock Movement Audit Trail</h3>
                      <button
                        onClick={() => exportToCSV(inventoryData.stockMovementHistory, "Inventory_Movements")}
                        className="fef-btn fef-btn-secondary py-1 px-3 text-xs flex items-center gap-1 no-print"
                      >
                        <FiDownload /> Export CSV
                      </button>
                    </div>
                    <div style={{ maxHeight: 280, overflowY: "auto" }}>
                      <table className="fef-table w-full text-xs">
                        <thead>
                          <tr>
                            <th>Product</th>
                            <th>Action</th>
                            <th>Qty</th>
                            <th>Ref Number</th>
                            <th>Timestamp</th>
                          </tr>
                        </thead>
                        <tbody>
                          {inventoryData.stockMovementHistory?.map((m, idx) => (
                            <tr key={idx}>
                              <td>{m.productName}</td>
                              <td>
                                <span className={`badge ${m.transactionType === "RECEIPT" ? "badge-success" : m.transactionType === "LOADING" ? "badge-danger" : "badge-warning"}`}>
                                  {m.transactionType}
                                </span>
                              </td>
                              <td className="font-semibold">{m.quantity?.toLocaleString()} L</td>
                              <td>{m.referenceNumber}</td>
                              <td>{new Date(m.timestamp).toLocaleString()}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Operations Tab */}
            {activeTab === "operations" && (
              <div>
                <h2 className="text-xl font-bold mb-4 print-only">Falcon Energy - Operations & Loading Performance</h2>
                
                {/* Operations Overview Sub-cards */}
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 20, marginBottom: 24 }}>
                  {loadingData && (
                    <div className="fef-panel" style={{ padding: 20 }}>
                      <h3>Loading Bay Analytics</h3>
                      <div className="grid grid-cols-2 gap-4 mt-4">
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Total Loaded Trucks</small>
                          <span className="font-bold text-lg">{loadingData.totalTrucksLoaded}</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Ambient loaded Volume</small>
                          <span className="font-bold text-base">{loadingData.totalAmbientVolume?.toLocaleString()} L</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Standard loaded Volume</small>
                          <span className="font-bold text-base text-primary">{loadingData.totalStandardVolume?.toLocaleString()} L</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Avg Loading Time</small>
                          <span className="font-bold text-base">{loadingData.averageLoadingDuration?.toFixed(1)} mins</span>
                        </div>
                      </div>
                    </div>
                  )}

                  {dispatchData && (
                    <div className="fef-panel" style={{ padding: 20 }}>
                      <h3>Truck Dispatching Analytics</h3>
                      <div className="grid grid-cols-2 gap-4 mt-4 mb-4">
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Total Dispatched Trucks</small>
                          <span className="font-bold text-lg text-primary">{dispatchData.totalDispatchedTrucks}</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Identified Delays</small>
                          <span className="font-bold text-lg text-danger">{dispatchData.dispatchDelays}</span>
                        </div>
                      </div>
                      <div style={{ height: 160 }}>
                        <ResponsiveContainer width="100%" height="100%">
                          <PieChart>
                            <Pie
                              data={dispatchData.dispatchStatusDistribution}
                              dataKey="count"
                              nameKey="status"
                              cx="50%"
                              cy="50%"
                              outerRadius={60}
                            >
                              {dispatchData.dispatchStatusDistribution?.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                              ))}
                            </Pie>
                            <Tooltip />
                          </PieChart>
                        </ResponsiveContainer>
                      </div>
                    </div>
                  )}

                  {deliveryData && (
                    <div className="fef-panel" style={{ padding: 20 }}>
                      <h3>Delivery Completion Analytics</h3>
                      <div className="grid grid-cols-2 gap-4 mt-4">
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Active Deliveries</small>
                          <span className="font-bold text-lg text-warning">{deliveryData.activeDeliveries}</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Completed Deliveries</small>
                          <span className="font-bold text-lg text-success">{deliveryData.deliveredTrucks}</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Pending Arrival</small>
                          <span className="font-bold text-lg">{deliveryData.pendingDeliveries}</span>
                        </div>
                        <div className="p-3 bg-background border rounded shadow-sm">
                          <small className="text-muted-foreground block text-xs">Avg Transit Duration</small>
                          <span className="font-bold text-base">{(deliveryData.averageDeliveryCompletionTime / 60)?.toFixed(1)} hrs</span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Finance Tab */}
            {activeTab === "finance" && financialData && (
              <div>
                <h2 className="text-xl font-bold mb-4 print-only">Falcon Energy - Financial Performance & Outstanding Bills</h2>
                <div className="fef-stat-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 16, marginBottom: 24 }}>
                  <StatCard label="Total Invoiced Amount" value={`$${financialData.totalInvoicedAmount?.toLocaleString()}`} icon={FiDollarSign} tone="primary" />
                  <StatCard label="Paid Invoiced Amount" value={`$${financialData.paidAmount?.toLocaleString()}`} icon={FiDollarSign} tone="success" />
                  <StatCard label="Outstanding Receivables" value={`$${financialData.outstandingAmount?.toLocaleString()}`} icon={FiFileText} tone="danger" />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: 20, marginBottom: 24 }}>
                  <div className="fef-panel" style={{ padding: 20 }}>
                    <h3>Financial Status Invoicing (Paid vs Outstanding)</h3>
                    <div style={{ height: 260, marginTop: 20 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart
                          data={[
                            { name: "Paid Invoices", value: financialData.paidAmount },
                            { name: "Outstanding Invoices", value: financialData.outstandingAmount }
                          ]}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip formatter={(v) => `$${v.toLocaleString()}`} />
                          <Legend />
                          <Bar dataKey="value" fill="#FFBB28" name="Invoiced Revenue ($)" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>

                  <div className="fef-panel" style={{ padding: 20 }}>
                    <h3>Revenue by Fuel Product Category</h3>
                    <div style={{ height: 260, marginTop: 20 }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={financialData.revenueByFuelProduct}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="productName" />
                          <YAxis />
                          <Tooltip formatter={(v) => `$${v.toLocaleString()}`} />
                          <Legend />
                          <Bar dataKey="revenue" fill="#82ca9d" name="Collected Revenue ($)" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* History Tab */}
            {activeTab === "history" && (
              <div className="fef-panel" style={{ padding: 20 }}>
                <div className="flex justify-between items-center mb-4">
                  <h3>Report Generation Audit Log</h3>
                  <button
                    onClick={() => exportToCSV(historyData, "Reports_Generation_History")}
                    className="fef-btn fef-btn-secondary py-1 px-3 text-xs flex items-center gap-1 no-print"
                  >
                    <FiDownload /> Export CSV
                  </button>
                </div>
                <div style={{ maxHeight: 400, overflowY: "auto" }}>
                  <table className="fef-table w-full text-xs">
                    <thead>
                      <tr>
                        <th>Report Number</th>
                        <th>Type</th>
                        <th>Generated By</th>
                        <th>Generated At</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {historyData.map(h => (
                        <tr key={h.id}>
                          <td className="font-semibold text-primary">{h.reportNumber}</td>
                          <td>{h.reportType}</td>
                          <td>{h.generatedBy}</td>
                          <td>{new Date(h.generatedAt).toLocaleString()}</td>
                          <td>
                            <span className={`badge ${h.status === "GENERATED" ? "badge-success" : "badge-danger"}`}>
                              {h.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}
      </DashboardLayout>
    </RouteGuard>
  );
}
