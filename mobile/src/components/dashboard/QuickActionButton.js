import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";

export default function QuickActionButton({ icon, label, onPress, badgeCount }) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.button, pressed && styles.pressed]}
    >
      <View style={styles.iconContainer}>
        <View style={styles.icon}>{icon}</View>
        {badgeCount > 0 && (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{badgeCount}</Text>
          </View>
        )}
      </View>
      <Text numberOfLines={2} style={styles.label}>
        {label}
      </Text>
      <Ionicons color={colors.muted} name="chevron-forward" size={20} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: "center",
    backgroundColor: colors.white,
    borderColor: colors.border,
    borderRadius: 14,
    borderWidth: 1,
    flex: 1,
    flexDirection: "row",
    minHeight: 76,
    paddingHorizontal: 16,
  },
  iconContainer: {
    position: "relative",
  },
  icon: {
    alignItems: "center",
    backgroundColor: "#EFF6FF",
    borderRadius: 17,
    height: 34,
    justifyContent: "center",
    marginRight: 12,
    width: 34,
  },
  badge: {
    position: "absolute",
    top: -5,
    right: -5,
    backgroundColor: colors.danger,
    borderRadius: 9,
    width: 18,
    height: 18,
    justifyContent: "center",
    alignItems: "center",
  },
  badgeText: {
    color: colors.white,
    fontSize: 10,
    fontWeight: "700",
  },
  label: {
    color: colors.text,
    fontSize: 12,
    fontWeight: "600",
    flex: 1,
  },
  pressed: {
    opacity: 0.78,
  },
});
