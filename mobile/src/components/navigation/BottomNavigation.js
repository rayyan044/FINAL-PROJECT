import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import colors from "../../constants/colors";

export default function BottomNavigation({ navigation, unreadCount = 0, activeRoute = "Dashboard" }) {
  const insets = useSafeAreaInsets();
  const bottomInset = insets.bottom > 0 ? insets.bottom : 12;

  const tabs = [
    { label: "Home", route: "Dashboard", icons: ["home-outline", "home"] },
    { label: "Notifications", route: "Notifications", icons: ["notifications-outline", "notifications"] },
    { label: "Profile", route: "Profile", icons: ["person-outline", "person"] },
  ];

  return <View pointerEvents="box-none" style={styles.outer}><View style={[styles.container, { height: 56 + bottomInset, paddingBottom: bottomInset }]}>{tabs.map((tab) => {
    const active = activeRoute === tab.route;
    return <Pressable accessibilityLabel={`Open ${tab.label}`} accessibilityRole="button" key={tab.route} onPress={() => !active && navigation.navigate(tab.route)} style={styles.tabButton}>
      <View style={styles.iconWrapper}><Ionicons color={active ? colors.primary : colors.gray} name={active ? tab.icons[1] : tab.icons[0]} size={22} />
        {tab.route === "Notifications" && unreadCount > 0 ? <View style={styles.badge}><Text style={styles.badgeText}>{unreadCount > 9 ? "9+" : unreadCount}</Text></View> : null}
      </View>
      <Text style={[styles.label, active && styles.activeLabel]}>{tab.label}</Text>
    </Pressable>;
  })}</View></View>;
}

const styles = StyleSheet.create({
  activeLabel: { color: colors.primary, fontWeight: "800" },
  badge: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.white, borderRadius: 9, borderWidth: 1.5, height: 17, justifyContent: "center", minWidth: 17, paddingHorizontal: 3, position: "absolute", right: -11, top: -5 },
  badgeText: { color: colors.white, fontSize: 8, fontWeight: "800" },
  container: { alignItems: "center", backgroundColor: colors.white, borderTopColor: colors.border, borderTopWidth: 1, elevation: 10, flexDirection: "row", justifyContent: "space-around", paddingTop: 8, shadowColor: colors.navy, shadowOffset: { width: 0, height: -4 }, shadowOpacity: 0.06, shadowRadius: 8 },
  iconWrapper: { position: "relative" },
  label: { color: colors.gray, fontSize: 11, fontWeight: "600", marginTop: 4 },
  outer: { bottom: 0, left: 0, position: "absolute", right: 0, zIndex: 1000 },
  tabButton: { alignItems: "center", flex: 1, justifyContent: "center", paddingVertical: 4 },
});
