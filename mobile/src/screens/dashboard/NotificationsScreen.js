import { Ionicons } from "@expo/vector-icons";
import { useCallback, useEffect, useMemo, useState } from "react";
import { FlatList, Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";

import BottomNavigation from "../../components/navigation/BottomNavigation";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/ScreenState";
import colors from "../../constants/colors";
import { radius, shadow, spacing } from "../../constants/theme";
import { getNotifications, markAsRead } from "../../services/notificationService";

const filters = ["All", "Unread"];

function isRead(notification) { return notification.read !== undefined ? notification.read : notification.isRead; }

export default function NotificationsScreen({ navigation }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("All");

  const loadNotifications = useCallback(async () => {
    try { setError(""); setLoading(true); setNotifications(await getNotifications()); }
    catch (err) { setError(err.message || "Failed to load notifications."); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadNotifications(); }, [loadNotifications]);

  const visibleNotifications = useMemo(() => filter === "Unread" ? notifications.filter((item) => !isRead(item)) : notifications, [filter, notifications]);

  const handleNotificationPress = async (item) => {
    if (!isRead(item)) {
      try { await markAsRead(item.id); setNotifications((previous) => previous.map((entry) => entry.id === item.id ? { ...entry, read: true, isRead: true } : entry)); }
      catch (markError) { console.error("Failed to mark notification as read", markError); }
    }
    if (item.type === "DELIVERY_ASSIGNED" && item.deliveryId) navigation.navigate("DeliveryDetails", { deliveryId: item.deliveryId });
  };

  if (loading) return <SafeAreaView style={styles.safeArea}><LoadingState label="Loading notifications…" /></SafeAreaView>;
  if (error) return <SafeAreaView style={styles.safeArea}><ErrorState message={error} onRetry={loadNotifications} title="Notifications unavailable" /></SafeAreaView>;

  return <SafeAreaView style={styles.safeArea}>
    <View style={styles.header}><Pressable accessibilityLabel="Back" hitSlop={10} onPress={() => navigation.goBack()} style={styles.back}><Ionicons color={colors.text} name="arrow-back" size={24} /></Pressable><View style={styles.headerCopy}><Text style={styles.title}>Notifications</Text><Text style={styles.subtitle}>{notifications.filter((item) => !isRead(item)).length ? "You have unread updates" : "Stay up to date with deliveries"}</Text></View></View>
    <View style={styles.tabs}>{filters.map((item) => <Pressable key={item} onPress={() => setFilter(item)} style={[styles.tab, filter === item && styles.activeTab]}><Text style={[styles.tabText, filter === item && styles.activeTabText]}>{item}</Text></Pressable>)}</View>
    <FlatList
      contentContainerStyle={visibleNotifications.length ? styles.list : styles.emptyList}
      data={visibleNotifications}
      keyExtractor={(item) => String(item.id)}
      ListEmptyComponent={<EmptyState icon="notifications-off-outline" message={filter === "Unread" ? "There are no unread notifications." : "You have no notifications at this time."} title="All caught up!" />}
      onRefresh={loadNotifications}
      refreshing={loading}
      renderItem={({ item }) => <NotificationCard item={item} onPress={() => handleNotificationPress(item)} />}
      showsVerticalScrollIndicator={false}
    />
    <BottomNavigation activeRoute="Notifications" navigation={navigation} unreadCount={notifications.filter((item) => !isRead(item)).length} />
  </SafeAreaView>;
}

function NotificationCard({ item, onPress }) {
  const read = isRead(item);
  return <Pressable onPress={onPress} style={({ pressed }) => [styles.card, !read && styles.unreadCard, pressed && styles.pressed]}><View style={[styles.icon, !read && styles.unreadIcon]}><Ionicons color={!read ? colors.primary : colors.gray} name={!read ? "mail-unread-outline" : "mail-open-outline"} size={21} /></View><View style={styles.cardCopy}><View style={styles.cardTop}><Text numberOfLines={1} style={[styles.cardTitle, !read && styles.bold]}>{item.title}</Text>{!read ? <View style={styles.dot} /> : null}</View><Text numberOfLines={3} style={styles.message}>{item.message}</Text>{item.createdAt ? <Text style={styles.date}>{new Date(item.createdAt).toLocaleString()}</Text> : null}</View></Pressable>;
}

const styles = StyleSheet.create({
  activeTab: { backgroundColor: colors.primary }, activeTabText: { color: colors.white }, back: { marginRight: spacing.sm, padding: spacing.sm }, bold: { fontWeight: "800" }, card: { alignItems: "flex-start", backgroundColor: colors.white, borderColor: colors.border, borderRadius: radius.lg, borderWidth: 1, flexDirection: "row", marginBottom: spacing.md, padding: spacing.lg, ...shadow.card }, cardCopy: { flex: 1 }, cardTitle: { color: colors.text, flex: 1, fontSize: 15, fontWeight: "700" }, cardTop: { alignItems: "center", flexDirection: "row" }, date: { color: colors.muted, fontSize: 12, marginTop: spacing.sm }, dot: { backgroundColor: colors.brightBlue, borderRadius: 5, height: 9, marginLeft: spacing.sm, width: 9 }, emptyList: { flexGrow: 1, paddingBottom: 86 }, header: { alignItems: "center", flexDirection: "row", paddingHorizontal: spacing.lg, paddingTop: spacing.md }, headerCopy: { flex: 1 }, icon: { alignItems: "center", backgroundColor: colors.surfaceMuted, borderRadius: 14, height: 42, justifyContent: "center", marginRight: spacing.md, width: 42 }, list: { padding: spacing.lg, paddingBottom: 96 }, message: { color: colors.gray, fontSize: 13, lineHeight: 19, marginTop: spacing.xs }, pressed: { opacity: 0.78 }, safeArea: { backgroundColor: colors.background, flex: 1 }, subtitle: { color: colors.gray, fontSize: 13, marginTop: 3 }, tab: { borderRadius: radius.pill, paddingHorizontal: 17, paddingVertical: 9 }, tabText: { color: colors.gray, fontSize: 13, fontWeight: "700" }, tabs: { alignSelf: "flex-start", backgroundColor: "#EAF0F8", borderRadius: radius.pill, flexDirection: "row", marginLeft: spacing.xl, marginTop: spacing.lg, padding: 3 }, title: { color: colors.text, fontSize: 22, fontWeight: "800" }, unreadCard: { backgroundColor: "#F8FBFF", borderColor: "#BFDBFE" }, unreadIcon: { backgroundColor: "#DBEAFE" },
});
