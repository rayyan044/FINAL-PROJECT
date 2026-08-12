import { Ionicons } from "@expo/vector-icons";
import { useEffect, useRef } from "react";
import { Animated, Dimensions, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import colors from "../../constants/colors";

const { width: windowWidth, height: windowHeight } = Dimensions.get("window");

export default function BottomNavigation({
  navigation,
  unreadCount = 0,
  dropdownOpen,
  onProfilePress,
  onCloseDropdown,
  onMyProfile,
  onLogout,
  activeRoute = "Dashboard",
}) {
  const insets = useSafeAreaInsets();
  
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

  const bottomInset = insets.bottom > 0 ? insets.bottom : 12;
  const barHeight = 56 + bottomInset;

  return (
    <View 
      style={[
        styles.outerContainer, 
        dropdownOpen ? styles.fullScreenContainer : styles.bottomBarContainer
      ]}
      pointerEvents={dropdownOpen ? "auto" : "box-none"}
    >
      {dropdownOpen && (
        <Pressable
          accessibilityLabel="Close profile options"
          onPress={onCloseDropdown}
          style={styles.overlay}
        />
      )}

      {dropdownOpen && (
        <Animated.View
          style={[
            styles.dropdown,
            {
              opacity: fadeAnim,
              transform: [{ scale: scaleAnim }],
              bottom: barHeight + 8,
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

      <View
        style={[
          styles.container,
          {
            paddingBottom: bottomInset,
            height: barHeight,
          },
        ]}
      >
        <Pressable
          accessibilityLabel="Go to Dashboard"
          accessibilityRole="button"
          onPress={() => {
            if (activeRoute !== "Dashboard") {
              navigation.navigate("Dashboard");
            }
          }}
          style={styles.tabButton}
        >
          <Ionicons
            color={activeRoute === "Dashboard" ? colors.primary : colors.gray}
            name={activeRoute === "Dashboard" ? "home" : "home-outline"}
            size={22}
          />
          <Text
            style={[
              styles.tabLabel,
              activeRoute === "Dashboard" && styles.activeTabLabel,
            ]}
          >
            Home
          </Text>
        </Pressable>

        <Pressable
          accessibilityLabel="Open notifications"
          accessibilityRole="button"
          onPress={() => {
            navigation.navigate("Notifications");
          }}
          style={styles.tabButton}
        >
          <View style={styles.iconWrapper}>
            <Ionicons
              color={activeRoute === "Notifications" ? colors.primary : colors.gray}
              name={activeRoute === "Notifications" ? "notifications" : "notifications-outline"}
              size={22}
            />
            {unreadCount > 0 && (
              <View style={styles.badgeContainer}>
                <Text style={styles.badgeText}>
                  {unreadCount > 9 ? "9+" : unreadCount}
                </Text>
              </View>
            )}
          </View>
          <Text
            style={[
              styles.tabLabel,
              activeRoute === "Notifications" && styles.activeTabLabel,
            ]}
          >
            Notifications
          </Text>
        </Pressable>

        <Pressable
          accessibilityLabel="Open profile options"
          accessibilityRole="button"
          onPress={onProfilePress}
          style={styles.tabButton}
        >
          <Ionicons
            color={dropdownOpen || activeRoute === "Profile" ? colors.primary : colors.gray}
            name={dropdownOpen || activeRoute === "Profile" ? "person" : "person-outline"}
            size={22}
          />
          <Text
            style={[
              styles.tabLabel,
              (dropdownOpen || activeRoute === "Profile") && styles.activeTabLabel,
            ]}
          >
            Profile
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  outerContainer: {
    position: "absolute",
    left: 0,
    right: 0,
    zIndex: 1000,
  },
  bottomBarContainer: {
    bottom: 0,
  },
  fullScreenContainer: {
    top: 0,
    bottom: 0,
  },
  container: {
    flexDirection: "row",
    backgroundColor: colors.white,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    justifyContent: "space-around",
    alignItems: "center",
    shadowColor: "#0F172A",
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 10,
    position: "absolute",
    bottom: 0,
    left: 0,
    right: 0,
    paddingTop: 8,
  },
  tabButton: {
    alignItems: "center",
    justifyContent: "center",
    flex: 1,
    paddingVertical: 4,
  },
  tabLabel: {
    fontSize: 11,
    color: colors.gray,
    marginTop: 4,
    fontWeight: "600",
  },
  activeTabLabel: {
    color: colors.primary,
    fontWeight: "700",
  },
  iconWrapper: {
    position: "relative",
  },
  badgeContainer: {
    position: "absolute",
    top: -4,
    right: -10,
    backgroundColor: colors.danger,
    borderRadius: 8,
    minWidth: 16,
    height: 16,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 3,
    borderWidth: 1.5,
    borderColor: colors.white,
  },
  badgeText: {
    color: colors.white,
    fontSize: 8,
    fontWeight: "700",
    textAlign: "center",
  },
  dropdown: {
    position: "absolute",
    right: 24,
    backgroundColor: colors.white,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    paddingVertical: 4,
    width: 140,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 5,
    zIndex: 1001,
  },
  dropdownItem: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 12,
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
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "transparent",
    zIndex: 999,
  },
});
