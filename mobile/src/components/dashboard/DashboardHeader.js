import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";

export default function DashboardHeader({ driver, greeting, onNotifications, unreadCount = 0 }) {
  const vehicle = driver?.assignedVehicle;
  const vehicleName = vehicle?.truckNumber || vehicle?.plateNumber;

  return (
    <View style={styles.header}>
      <View style={styles.headerGlow} />
      <View style={styles.copy}>
        <Text style={styles.greeting}>{greeting}</Text>
        <Text numberOfLines={1} style={styles.name}>
          {driver?.driverName || "Driver"}
        </Text>
        {vehicleName ? (
          <View style={styles.vehicleRow}>
            <Ionicons color={colors.accent} name="car-outline" size={15} />
            <Text numberOfLines={1} style={styles.vehicle}>
              {vehicleName}
            </Text>
          </View>
        ) : null}
      </View>
      <Pressable accessibilityLabel="Open notifications" onPress={onNotifications} style={styles.bell}>
        <Ionicons color={colors.white} name="notifications-outline" size={22} />
        {unreadCount > 0 ? <View style={styles.unread}><Text style={styles.unreadText}>{unreadCount > 9 ? "9+" : unreadCount}</Text></View> : null}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bell: { alignItems: "center", backgroundColor: "rgba(255,255,255,0.14)", borderColor: "rgba(255,255,255,0.18)", borderRadius: 16, borderWidth: 1, height: 42, justifyContent: "center", position: "relative", width: 42 },
  copy: { flex: 1, paddingRight: 14, zIndex: 1 },
  greeting: { color: "#CBD5E1", fontSize: 14, fontWeight: "600" },
  header: {
    alignItems: "center",
    backgroundColor: colors.primary,
    flexDirection: "row",
    minHeight: 174,
    paddingBottom: 28,
    paddingHorizontal: 24,
    paddingTop: 32,
    overflow: "hidden",
    zIndex: 10,
  },
  headerGlow: { backgroundColor: colors.brightBlue, borderRadius: 160, height: 290, opacity: 0.36, position: "absolute", right: -110, top: -152, width: 290 },
  name: { color: colors.white, fontSize: 28, fontWeight: "700", marginTop: 4 },
  vehicle: { color: "#E2E8F0", fontSize: 13, marginLeft: 6 },
  vehicleRow: { alignItems: "center", flexDirection: "row", marginTop: 10 },
  unread: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.white, borderRadius: 10, borderWidth: 1.5, height: 18, justifyContent: "center", position: "absolute", right: -5, top: -5, width: 18 },
  unreadText: { color: colors.white, fontSize: 9, fontWeight: "800" },
});
