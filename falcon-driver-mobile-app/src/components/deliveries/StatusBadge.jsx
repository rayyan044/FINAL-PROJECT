import { StyleSheet, Text, View } from "react-native";

const statusStyles = {
  READY: { backgroundColor: "#DBEAFE", color: "#1D4ED8" },
  IN_TRANSIT: { backgroundColor: "#FEF3C7", color: "#B45309" },
  ARRIVED: { backgroundColor: "#E0E7FF", color: "#4338CA" },
  OFFLOADING: { backgroundColor: "#FCE7F3", color: "#BE185D" },
  DELIVERED: { backgroundColor: "#D1FAE5", color: "#047857" },
  CANCELLED: { backgroundColor: "#FEE2E2", color: "#B91C1C" },
};

function labelFor(status) {
  return String(status || "UNKNOWN").replaceAll("_", " ");
}

export default function StatusBadge({ status }) {
  const normalizedStatus = String(status || "UNKNOWN").toUpperCase();
  const palette = statusStyles[normalizedStatus] || { backgroundColor: "#E5E7EB", color: "#4B5563" };

  return (
    <View style={[styles.badge, { backgroundColor: palette.backgroundColor }]}>
      <Text numberOfLines={1} style={[styles.label, { color: palette.color }]}>{labelFor(normalizedStatus)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { borderRadius: 999, maxWidth: "50%", paddingHorizontal: 9, paddingVertical: 5 },
  label: { fontSize: 10, fontWeight: "800", letterSpacing: 0.25 },
});
