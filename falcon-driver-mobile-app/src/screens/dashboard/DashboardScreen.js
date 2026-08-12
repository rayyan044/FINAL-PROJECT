import { Ionicons } from "@expo/vector-icons";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Alert, Pressable, RefreshControl, SafeAreaView, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import DashboardHeader from "../../components/dashboard/DashboardHeader";
import DeliveryCard from "../../components/dashboard/DeliveryCard";
import QuickActionButton from "../../components/dashboard/QuickActionButton";
import SummaryCard from "../../components/dashboard/SummaryCard";
import BottomNavigation from "../../components/navigation/BottomNavigation";
import colors from "../../constants/colors";
import { getDashboard } from "../../services/dashboardService";
import { useAuth } from "../../context/AuthContext";

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
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const { logout } = useAuth();

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
      setDropdownOpen(false); // reset dropdown state when screen is refocused
    });

    return unsubscribe;
  }, [navigation, loadDashboard]);

  if (loading) {
    return <LoadingView />;
  }

  if (error && !dashboard) {
    return <ErrorView message={error} onRetry={() => loadDashboard()} />;
  }

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
          />
          <View style={styles.body}>
            {error ? <Text style={styles.refreshError}>{error}</Text> : null}
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
          dropdownOpen={dropdownOpen}
          onProfilePress={() => setDropdownOpen((prev) => !prev)}
          onCloseDropdown={() => setDropdownOpen(false)}
          onMyProfile={() => {
            setDropdownOpen(false);
            navigation.navigate("Profile");
          }}
          onLogout={async () => {
            setDropdownOpen(false);
            try {
              await logout();
            } catch (err) {
              Alert.alert("Error", err.message || "Logout failed. Please try again.");
            }
          }}
          activeRoute="Dashboard"
        />
      </View>
    </SafeAreaView>
  );
}

function LoadingView() {
  return <View style={styles.centered}><ActivityIndicator color={colors.primary} size="large" /><Text style={styles.loadingText}>Loading your dashboard…</Text></View>;
}

function ErrorView({ message, onRetry }) {
  return <SafeAreaView style={styles.centered}><Ionicons color={colors.danger} name="cloud-offline-outline" size={42} /><Text style={styles.errorTitle}>Dashboard unavailable</Text><Text style={styles.errorMessage}>{message}</Text><Pressable onPress={onRetry} style={styles.retryButton}><Text style={styles.retryText}>Try again</Text></Pressable></SafeAreaView>;
}

function EmptyState({ onRefresh }) {
  return <View style={styles.empty}><Ionicons color={colors.primary} name="file-tray-outline" size={36} /><Text style={styles.emptyTitle}>No deliveries assigned.</Text><Text style={styles.emptyText}>Pull down or refresh to check for new work.</Text><Pressable onPress={onRefresh} style={styles.retryButton}><Text style={styles.retryText}>Refresh</Text></Pressable></View>;
}

const styles = StyleSheet.create({
  body: { padding: 20 },
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
  refreshError: { color: colors.danger, fontSize: 13, marginBottom: 12, textAlign: "center" },
  retryButton: { backgroundColor: colors.primary, borderRadius: 10, marginTop: 20, paddingHorizontal: 22, paddingVertical: 12 },
  retryText: { color: colors.white, fontSize: 14, fontWeight: "700" },
  safeArea: { backgroundColor: colors.primary, flex: 1 },
  sectionTitle: { color: colors.text, fontSize: 18, fontWeight: "700", marginBottom: 13, marginTop: 24 },
  summaryGrid: { flexDirection: "row", flexWrap: "wrap", gap: 12, justifyContent: "space-between" },
});

