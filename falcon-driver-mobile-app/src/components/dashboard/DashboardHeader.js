import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";

export default function DashboardHeader({ driver, greeting, onProfilePress }) {
  const vehicle = driver?.assignedVehicle;
  const vehicleName = vehicle?.truckNumber || vehicle?.plateNumber;

  return (
    <View style={styles.header}>
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
      <Pressable
        accessibilityLabel="Open profile"
        accessibilityRole="button"
        onPress={onProfilePress}
        style={({ pressed }) => [styles.profileButton, pressed && styles.pressed]}
      >
        <Ionicons color={colors.primary} name="person-outline" size={24} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  copy: { flex: 1, paddingRight: 14 },
  greeting: { color: "#CBD5E1", fontSize: 14, fontWeight: "600" },
  header: {
    alignItems: "center",
    backgroundColor: colors.primary,
    flexDirection: "row",
    minHeight: 154,
    paddingBottom: 28,
    paddingHorizontal: 24,
    paddingTop: 32,
  },
  name: { color: colors.white, fontSize: 28, fontWeight: "700", marginTop: 4 },
  pressed: { opacity: 0.75 },
  profileButton: {
    alignItems: "center",
    backgroundColor: colors.white,
    borderRadius: 22,
    height: 44,
    justifyContent: "center",
    width: 44,
  },
  vehicle: { color: "#E2E8F0", fontSize: 13, marginLeft: 6 },
  vehicleRow: { alignItems: "center", flexDirection: "row", marginTop: 10 },
});
