import { Ionicons } from "@expo/vector-icons";
import { useEffect, useRef } from "react";
import { Animated, Dimensions, Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";

const { width: windowWidth, height: windowHeight } = Dimensions.get("window");

export default function DashboardHeader({
  driver,
  greeting,
  unreadCount = 0,
  onNotificationsPress,
  onProfilePress,
  dropdownOpen,
  onCloseDropdown,
  onMyProfile,
  onLogout,
}) {
  const vehicle = driver?.assignedVehicle;
  const vehicleName = vehicle?.truckNumber || vehicle?.plateNumber;

  const fadeAnim = useRef(new Animated.Value(0)).current;
  const scaleAnim = useRef(new Animated.Value(0.95)).current;

  useEffect(() => {
    if (dropdownOpen) {
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 150,
          useNativeDriver: true,
        }),
        Animated.timing(scaleAnim, {
          toValue: 1,
          duration: 150,
          useNativeDriver: true,
        }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 0,
          duration: 100,
          useNativeDriver: true,
        }),
        Animated.timing(scaleAnim, {
          toValue: 0.95,
          duration: 100,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [dropdownOpen, fadeAnim, scaleAnim]);

  return (
    <View style={styles.header}>
      {dropdownOpen && (
        <Pressable
          accessibilityLabel="Close profile options"
          onPress={onCloseDropdown}
          style={styles.overlay}
        />
      )}
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
      <View style={styles.actionsContainer}>
        <Pressable
          accessibilityLabel="Open notifications"
          accessibilityRole="button"
          onPress={onNotificationsPress}
          style={({ pressed }) => [styles.iconButton, styles.bellButton, pressed && styles.pressed]}
        >
          <Ionicons color={colors.primary} name="notifications-outline" size={24} />
          {unreadCount > 0 && (
            <View style={styles.badgeContainer}>
              <Text style={styles.badgeText}>
                {unreadCount > 9 ? "9+" : unreadCount}
              </Text>
            </View>
          )}
        </Pressable>
        <View style={styles.profileWrapper}>
          <Pressable
            accessibilityLabel="Open profile options"
            accessibilityRole="button"
            onPress={onProfilePress}
            style={({ pressed }) => [styles.iconButton, pressed && styles.pressed]}
          >
            <Ionicons color={colors.primary} name="person-outline" size={24} />
          </Pressable>
        </View>
      </View>
      {dropdownOpen && (
        <Animated.View
          style={[
            styles.dropdown,
            {
              opacity: fadeAnim,
              transform: [{ scale: scaleAnim }],
            },
          ]}
        >
          <Pressable
            accessibilityLabel="My Profile"
            accessibilityRole="button"
            onPress={onMyProfile}
            style={({ pressed }) => [styles.dropdownItem, pressed && styles.dropdownItemPressed]}
          >
            <Ionicons color={colors.primary} name="person-outline" size={18} />
            <Text style={styles.dropdownText}>My Profile</Text>
          </Pressable>
          <View style={styles.dropdownDivider} />
          <Pressable
            accessibilityLabel="Log out"
            accessibilityRole="button"
            onPress={onLogout}
            style={({ pressed }) => [styles.dropdownItem, pressed && styles.dropdownItemPressed]}
          >
            <Ionicons color={colors.danger} name="log-out-outline" size={18} />
            <Text style={[styles.dropdownText, { color: colors.danger }]}>Logout</Text>
          </Pressable>
        </Animated.View>
      )}
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
    zIndex: 10,
  },
  name: { color: colors.white, fontSize: 28, fontWeight: "700", marginTop: 4 },
  pressed: { opacity: 0.75 },
  vehicle: { color: "#E2E8F0", fontSize: 13, marginLeft: 6 },
  vehicleRow: { alignItems: "center", flexDirection: "row", marginTop: 10 },
  actionsContainer: {
    flexDirection: "row",
    alignItems: "center",
    zIndex: 20,
  },
  iconButton: {
    alignItems: "center",
    backgroundColor: colors.white,
    borderRadius: 22,
    height: 44,
    justifyContent: "center",
    width: 44,
    position: "relative",
  },
  bellButton: {
    marginRight: 12,
  },
  profileWrapper: {
    position: "relative",
    zIndex: 30,
  },
  badgeContainer: {
    position: "absolute",
    top: -4,
    right: -4,
    backgroundColor: colors.danger,
    borderRadius: 9,
    minWidth: 18,
    height: 18,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 4,
    borderWidth: 1.5,
    borderColor: colors.white,
  },
  badgeText: {
    color: colors.white,
    fontSize: 9,
    fontWeight: "700",
    textAlign: "center",
  },
  dropdown: {
    position: "absolute",
    top: 80,
    right: 24,
    backgroundColor: colors.white,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    paddingVertical: 4,
    width: 140,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 5,
    zIndex: 1000,
  },
  dropdownItem: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  dropdownItemPressed: {
    backgroundColor: "#F3F4F6",
  },
  dropdownText: {
    color: colors.text,
    fontSize: 14,
    fontWeight: "600",
    marginLeft: 8,
  },
  dropdownDivider: {
    height: 1,
    backgroundColor: colors.border,
    marginHorizontal: 8,
  },
  overlay: {
    position: "absolute",
    width: windowWidth * 2,
    height: windowHeight * 2,
    top: -windowHeight,
    left: -windowWidth,
    backgroundColor: "transparent",
    zIndex: 999,
  },
});
