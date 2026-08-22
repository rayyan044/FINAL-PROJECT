import { Ionicons } from "@expo/vector-icons";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ActivityIndicator, FlatList, Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";

import DeliveryCard from "../../components/deliveries/DeliveryCard";
import SearchBar from "../../components/deliveries/SearchBar";
import colors from "../../constants/colors";
import { radius, spacing } from "../../constants/theme";
import { getMyDeliveries } from "../../services/deliveryService";

const INACTIVE_STATUSES = new Set(["DELIVERED", "CANCELLED"]);

function dateValue(value, fallback) {
  const timestamp = value ? new Date(value).getTime() : Number.NaN;
  return Number.isNaN(timestamp) ? fallback : timestamp;
}

function sortDeliveries(deliveries) {
  return [...deliveries].sort((first, second) => {
    const firstInactive = INACTIVE_STATUSES.has(String(first.currentStatus || "").toUpperCase());
    const secondInactive = INACTIVE_STATUSES.has(String(second.currentStatus || "").toUpperCase());
    if (firstInactive !== secondInactive) return firstInactive ? 1 : -1;

    const scheduledDifference = dateValue(first.scheduledDeliveryDate, Number.MAX_SAFE_INTEGER) - dateValue(second.scheduledDeliveryDate, Number.MAX_SAFE_INTEGER);
    if (scheduledDifference !== 0) return scheduledDifference;

    return dateValue(second.assignedAt, 0) - dateValue(first.assignedAt, 0);
  });
}

export default function MyDeliveriesScreen({ navigation }) {
  const [deliveries, setDeliveries] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("All");

  const loadDeliveries = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);

    try {
      setDeliveries(await getMyDeliveries());
      setError("");
    } catch (loadError) {
      setError(loadError.message || "We could not load your deliveries. Please try again.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { loadDeliveries(); }, [loadDeliveries]);

  const visibleDeliveries = useMemo(() => {
    const term = query.trim().toLocaleLowerCase();
    const matching = !term
      ? deliveries
      : deliveries.filter((delivery) => [delivery.deliveryNoteNumber, delivery.customerName, delivery.destination]
        .some((value) => String(value || "").toLocaleLowerCase().includes(term)));
    return sortDeliveries(matching).filter((delivery) => {
      const status = String(delivery.currentStatus || "").toUpperCase();
      if (filter === "Active") return !["DELIVERED", "COMPLETED", "CANCELLED"].includes(status);
      if (filter === "Completed") return ["DELIVERED", "COMPLETED"].includes(status);
      return true;
    });
  }, [deliveries, query, filter]);

  if (loading) return <LoadingView />;
  if (error && deliveries.length === 0) return <ErrorView message={error} onRetry={loadDeliveries} />;

  return (
    <SafeAreaView style={styles.safeArea}>
      <FlatList
        contentContainerStyle={styles.listContent}
        data={visibleDeliveries}
        keyExtractor={(delivery, index) => String(delivery.deliveryId || delivery.deliveryNoteNumber || index)}
        ListEmptyComponent={<EmptyState hasSearch={Boolean(query.trim())} onRefresh={() => loadDeliveries(true)} />}
        ListHeaderComponent={<><View style={styles.header}><Pressable accessibilityLabel="Back to dashboard" accessibilityRole="button" hitSlop={8} onPress={() => navigation.goBack()} style={styles.backButton}><Ionicons color={colors.text} name="arrow-back" size={24} /></Pressable><View><Text style={styles.title}>My Deliveries</Text><Text style={styles.count}>{deliveries.length} total deliveries</Text></View></View><SearchBar onChangeText={setQuery} value={query} /><View style={styles.tabs}>{["All", "Active", "Completed"].map((item) => <Pressable key={item} onPress={() => setFilter(item)} style={[styles.tab, filter === item && styles.activeTab]}><Text style={[styles.tabText, filter === item && styles.activeTabText]}>{item}</Text></Pressable>)}</View>{error ? <Text style={styles.refreshError}>{error}</Text> : null}</>}
        onRefresh={() => loadDeliveries(true)}
        refreshing={refreshing}
        renderItem={({ item }) => <DeliveryCard delivery={item} onViewDetails={() => navigation.navigate("DeliveryDetails", { deliveryId: item.deliveryId })} />}
        showsVerticalScrollIndicator={false}
      />
    </SafeAreaView>
  );
}

function LoadingView() { return <View style={styles.centered}><ActivityIndicator color={colors.primary} size="large" /><Text style={styles.loadingText}>Loading your deliveries…</Text></View>; }
function ErrorView({ message, onRetry }) { return <SafeAreaView style={styles.centered}><Ionicons color={colors.danger} name="cloud-offline-outline" size={42} /><Text style={styles.errorTitle}>Deliveries unavailable</Text><Text style={styles.errorText}>{message}</Text><Pressable onPress={onRetry} style={styles.retryButton}><Text style={styles.retryText}>Retry</Text></Pressable></SafeAreaView>; }
function EmptyState({ hasSearch, onRefresh }) { return <View style={styles.empty}><Ionicons color={colors.primary} name="file-tray-outline" size={38} /><Text style={styles.emptyTitle}>{hasSearch ? "No matching deliveries." : "No deliveries assigned."}</Text><Text style={styles.emptyText}>{hasSearch ? "Try a different search term." : "Refresh to check for new work."}</Text>{!hasSearch && <Pressable onPress={onRefresh} style={styles.retryButton}><Text style={styles.retryText}>Refresh</Text></Pressable>}</View>; }

const styles = StyleSheet.create({
  centered: { alignItems: "center", backgroundColor: colors.background, flex: 1, justifyContent: "center", padding: 28 },
  count: { color: colors.gray, fontSize: 14, marginTop: 4 },
  activeTab: { backgroundColor: colors.primary },
  activeTabText: { color: colors.white },
  empty: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.border, borderRadius: radius.lg, borderWidth: 1, marginTop: 24, padding: 30 },
  emptyText: { color: colors.gray, fontSize: 14, marginTop: 7, textAlign: "center" },
  emptyTitle: { color: colors.text, fontSize: 16, fontWeight: "700", marginTop: 13 },
  errorText: { color: colors.gray, fontSize: 14, lineHeight: 21, marginTop: 9, textAlign: "center" },
  errorTitle: { color: colors.text, fontSize: 20, fontWeight: "700", marginTop: 15 },
  backButton: { marginLeft: -7, marginRight: 9, padding: 4 },
  header: { alignItems: "center", flexDirection: "row", marginBottom: 18 },
  listContent: { flexGrow: 1, padding: 20 },
  loadingText: { color: colors.gray, fontSize: 14, marginTop: 14 },
  refreshError: { color: colors.danger, fontSize: 13, marginTop: 12, textAlign: "center" },
  retryButton: { backgroundColor: colors.primary, borderRadius: 10, marginTop: 20, paddingHorizontal: 22, paddingVertical: 12 },
  retryText: { color: colors.white, fontSize: 14, fontWeight: "700" },
  safeArea: { backgroundColor: colors.background, flex: 1 },
  tab: { borderRadius: 999, paddingHorizontal: 16, paddingVertical: 9 },
  tabText: { color: colors.gray, fontSize: 13, fontWeight: "700" },
  tabs: { alignSelf: "flex-start", backgroundColor: "#EAF0F8", borderRadius: 999, flexDirection: "row", marginTop: 14, padding: 3 },
  title: { color: colors.text, fontSize: 28, fontWeight: "800" },
});
