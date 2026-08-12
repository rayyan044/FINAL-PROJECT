import { Ionicons } from "@expo/vector-icons";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, FlatList, Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { getNotifications, markAsRead } from "../../services/notificationService";

export default function NotificationsScreen({ navigation }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadNotifications = useCallback(async () => {
    try {
      setError("");
      setLoading(true);
      const data = await getNotifications();
      setNotifications(data);
    } catch (err) {
      setError(err.message || "Failed to load notifications.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const handleNotificationPress = async (item) => {
    const isRead = item.read !== undefined ? item.read : item.isRead;
    if (!isRead) {
      try {
        await markAsRead(item.id);
        // Update local state to mark it as read
        setNotifications((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, read: true, isRead: true } : n))
        );
      } catch (err) {
        console.error("Failed to mark notification as read", err);
      }
    }
    if (item.type === "DELIVERY_ASSIGNED" && item.deliveryId) {
      navigation.navigate("DeliveryDetails", { deliveryId: item.deliveryId });
    }
  };

  const renderItem = ({ item }) => {
    const isRead = item.read !== undefined ? item.read : item.isRead;
    return (
      <Pressable
        onPress={() => handleNotificationPress(item)}
        style={[styles.notificationCard, !isRead && styles.unreadCard]}
      >
        <View style={styles.iconContainer}>
          <Ionicons
            color={isRead ? colors.gray : colors.primary}
            name={isRead ? "mail-open-outline" : "mail-unread-outline"}
            size={24}
          />
        </View>
        <View style={styles.textContainer}>
          <View style={styles.headerRow}>
            <Text style={[styles.title, !isRead && styles.unreadText]}>{item.title}</Text>
            {!isRead && <View style={styles.badge} />}
          </View>
          <Text style={styles.message}>{item.message}</Text>
          <Text style={styles.date}>
            {item.createdAt ? new Date(item.createdAt).toLocaleString() : ""}
          </Text>
        </View>
      </Pressable>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {loading ? (
          <View style={styles.centered}>
            <ActivityIndicator color={colors.primary} size="large" />
            <Text style={styles.loadingText}>Loading notifications...</Text>
          </View>
        ) : error ? (
          <View style={styles.centered}>
            <Ionicons color={colors.danger} name="alert-circle-outline" size={48} />
            <Text style={styles.errorTitle}>Failed to load</Text>
            <Text style={styles.errorMessage}>{error}</Text>
            <Pressable onPress={loadNotifications} style={styles.retryButton}>
              <Text style={styles.retryText}>Retry</Text>
            </Pressable>
          </View>
        ) : notifications.length === 0 ? (
          <View style={styles.centered}>
            <Ionicons color={colors.gray} name="notifications-off-outline" size={48} />
            <Text style={styles.emptyTitle}>All caught up!</Text>
            <Text style={styles.emptyText}>You have no notifications at this time.</Text>
          </View>
        ) : (
          <FlatList
            data={notifications}
            keyExtractor={(item) => String(item.id)}
            onRefresh={loadNotifications}
            refreshing={loading}
            renderItem={renderItem}
          />
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  badge: {
    backgroundColor: colors.primary,
    borderRadius: 5,
    height: 10,
    width: 10,
  },
  centered: {
    alignItems: "center",
    flex: 1,
    justifyContent: "center",
    padding: 24,
  },
  container: {
    flex: 1,
  },
  date: {
    color: colors.gray,
    fontSize: 12,
    marginTop: 6,
  },
  emptyText: {
    color: colors.gray,
    fontSize: 14,
    marginTop: 8,
    textAlign: "center",
  },
  emptyTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: "700",
    marginTop: 16,
  },
  errorMessage: {
    color: colors.gray,
    fontSize: 14,
    marginTop: 8,
    textAlign: "center",
  },
  errorTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: "700",
    marginTop: 16,
  },
  headerRow: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
  },
  iconContainer: {
    marginRight: 16,
  },
  loadingText: {
    color: colors.gray,
    fontSize: 14,
    marginTop: 8,
  },
  message: {
    color: colors.text,
    fontSize: 14,
    lineHeight: 20,
    marginTop: 4,
  },
  notificationCard: {
    alignItems: "flex-start",
    backgroundColor: colors.white,
    borderBottomColor: colors.border,
    borderBottomWidth: 1,
    flexDirection: "row",
    padding: 16,
  },
  retryButton: {
    backgroundColor: colors.primary,
    borderRadius: 8,
    marginTop: 16,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  retryText: {
    color: colors.white,
    fontWeight: "600",
  },
  safeArea: {
    backgroundColor: colors.background,
    flex: 1,
  },
  textContainer: {
    flex: 1,
  },
  title: {
    color: colors.text,
    fontSize: 16,
    fontWeight: "500",
  },
  unreadCard: {
    backgroundColor: "#F3F4F6",
  },
  unreadText: {
    color: colors.text,
    fontWeight: "700",
  },
});
