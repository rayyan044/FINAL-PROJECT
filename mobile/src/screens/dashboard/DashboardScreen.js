import { Ionicons } from "@expo/vector-icons";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, RefreshControl, SafeAreaView, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import DashboardHeader from "../../components/dashboard/DashboardHeader";
import DeliveryCard from "../../components/dashboard/DeliveryCard";
import QuickActionButton from "../../components/dashboard/QuickActionButton";
import SummaryCard from "../../components/dashboard/SummaryCard";
import BottomNavigation from "../../components/navigation/BottomNavigation";
import { ErrorState, LoadingState } from "../../components/ui/ScreenState";
import colors from "../../constants/colors";
import { getDashboard } from "../../services/dashboardService";

function greetingForCurrentTime() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good Morning";
  if (hour < 18) return "Good Afternoon";
  return "Good Evening";
}

export default function DashboardScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadDashboard = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);

    try {
      const data = await getDashboard();
      setDashboard(data);
      setError("");
    } catch (loadError) {
      setError(loadError.message || "We could not load your dashboard. Please try again.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();

    const unsubscribe = navigation.addListener("focus", () => {
      loadDashboard(true);
    });

    return unsubscribe;
  }, [navigation, loadDashboard]);

  if (loading) return <SafeAreaView style={styles.loadingSafe}><LoadingState label="Preparing your workspace…" /></SafeAreaView>;

  if (error && !dashboard) return <SafeAreaView style={styles.loadingSafe}><ErrorState message={error} onRetry={() => loadDashboard()} title="Dashboard unavailable" /></SafeAreaView>;

  const summary = dashboard.summary;
  const deliveries = dashboard.recentDeliveries;
  
  const bottomInset = insets.bottom > 0 ? insets.bottom : 12;
  const bottomNavHeight = 56 + bottomInset;

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <ScrollView
          contentContainerStyle={[
            styles.content,
            { paddingBottom: bottomNavHeight + 16 }
          ]}
          refreshControl={<RefreshControl colors={[colors.primary]} onRefresh={() => loadDashboard(true)} refreshing={refreshing} tintColor={colors.primary} />}
          showsVerticalScrollIndicator={false}
        >
          <DashboardHeader
            driver={dashboard.driver}
            greeting={greetingForCurrentTime()}
            onNotifications={() => navigation.navigate("Notifications")}
            unreadCount={dashboard.notifications?.unreadCount || 0}
          />
          <View style={styles.body}>
            {error ? <View style={styles.refreshError}><Ionicons color={colors.danger} name="information-circle-outline" size={16} /><Text style={styles.refreshErrorText}>{error}</Text></View> : null}
            <Text style={styles.sectionTitle}>Today’s overview</Text>
            <View style={styles.summaryGrid}>
              <SummaryCard icon={<Ionicons color={colors.primary} name="file-tray-full-outline" size={19} />} label="Assigned Deliveries" value={summary.assignedDeliveries} />
              <SummaryCard icon={<Ionicons color={colors.accent} name="time-outline" size={19} />} label="Pending" tone={colors.accent} value={summary.pendingDeliveries} />
              <SummaryCard icon={<Ionicons color={colors.success} name="navigate-outline" size={19} />} label="In Progress" tone={colors.success} value={summary.deliveriesInProgress} />
              <SummaryCard icon={<Ionicons color={colors.primary} name="checkmark-circle-outline" size={19} />} label="Completed Today" value={summary.completedToday} />
            </View>
            <Text style={styles.sectionTitle}>Quick actions</Text>
            <View style={styles.quickActions}>
              <QuickActionButton icon={<Ionicons color={colors.primary} name="list-outline" size={19} />} label="My Deliveries" onPress={() => navigation.navigate("Deliveries")} />
            </View>
            <Text style={styles.sectionTitle}>Recent deliveries</Text>
            {deliveries.length ? (
              deliveries.map((delivery) => <DeliveryCard delivery={delivery} key={delivery.deliveryId || delivery.deliveryNoteNumber} onOpen={() => navigation.navigate("DeliveryDetails", { deliveryId: delivery.deliveryId })} />)
            ) : (
              <EmptyState onRefresh={() => loadDashboard(true)} />
            )}
          </View>
        </ScrollView>
        <BottomNavigation
          navigation={navigation}
          unreadCount={dashboard.notifications?.unreadCount || 0}
          activeRoute="Dashboard"
        />
      </View>
    </SafeAreaView>
  );
}

function EmptyState({ onRefresh }) {
  return <View style={styles.empty}><Ionicons color={colors.primary} name="file-tray-outline" size={36} /><Text style={styles.emptyTitle}>No deliveries assigned.</Text><Text style={styles.emptyText}>Pull down or refresh to check for new work.</Text><Pressable onPress={onRefresh} style={styles.retryButton}><Text style={styles.retryText}>Refresh</Text></Pressable></View>;
}

const styles = StyleSheet.create({
  body: { backgroundColor: colors.background, borderTopLeftRadius: 28, borderTopRightRadius: 28, marginTop: -22, minHeight: 600, padding: 20, zIndex: 2 },
  centered: { alignItems: "center", backgroundColor: colors.background, flex: 1, justifyContent: "center", padding: 28 },
  container: { flex: 1 },
  content: { flexGrow: 1 },
  empty: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.border, borderRadius: 16, borderWidth: 1, padding: 30 },
  emptyText: { color: colors.gray, fontSize: 14, marginTop: 7, textAlign: "center" },
  emptyTitle: { color: colors.text, fontSize: 16, fontWeight: "700", marginTop: 13 },
  errorMessage: { color: colors.gray, fontSize: 14, lineHeight: 21, marginTop: 9, textAlign: "center" },
  errorTitle: { color: colors.text, fontSize: 20, fontWeight: "700", marginTop: 15 },
  loadingText: { color: colors.gray, fontSize: 14, marginTop: 14 },
  quickActions: { flexDirection: "row", gap: 10 },
  loadingSafe: { backgroundColor: colors.background, flex: 1 },
  refreshError: { alignItems: "center", backgroundColor: "#FEF2F2", borderColor: "#FECACA", borderRadius: 10, borderWidth: 1, flexDirection: "row", marginBottom: 12, padding: 10 },
  refreshErrorText: { color: colors.danger, flex: 1, fontSize: 13, marginLeft: 7 },
  retryButton: { backgroundColor: colors.primary, borderRadius: 10, marginTop: 20, paddingHorizontal: 22, paddingVertical: 12 },
  retryText: { color: colors.white, fontSize: 14, fontWeight: "700" },
  safeArea: { backgroundColor: colors.primary, flex: 1 },
  sectionTitle: { color: colors.text, fontSize: 18, fontWeight: "700", marginBottom: 13, marginTop: 24 },
  summaryGrid: { flexDirection: "row", flexWrap: "wrap", gap: 12, justifyContent: "space-between" },
});
