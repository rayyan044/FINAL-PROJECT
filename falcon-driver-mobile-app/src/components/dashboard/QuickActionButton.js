import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";

export default function QuickActionButton({ icon, label, onPress }) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.button, pressed && styles.pressed]}
    >
      <View style={styles.icon}>{icon}</View>
      <Text numberOfLines={2} style={styles.label}>
        {label}
      </Text>
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
    minHeight: 100,
    paddingHorizontal: 8,
    paddingVertical: 14,
  },
  icon: {
    alignItems: "center",
    backgroundColor: "#EFF6FF",
    borderRadius: 17,
    height: 34,
    justifyContent: "center",
    marginBottom: 9,
    width: 34,
  },
  label: {
    color: colors.text,
    fontSize: 12,
    fontWeight: "600",
    textAlign: "center",
  },
  pressed: {
    opacity: 0.78,
  },
});
