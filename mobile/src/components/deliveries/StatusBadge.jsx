import { StyleSheet, Text, View } from "react-native";

const statusStyles = {
  AVAILABLE: { backgroundColor: "#D1FAE5", color: "#047857" },
  ACTIVE: { backgroundColor: "#D1FAE5", color: "#047857" },
  ASSIGNED: { backgroundColor: "#DBEAFE", color: "#1D4ED8" },
  ACCEPTED: { backgroundColor: "#E0E7FF", color: "#4338CA" },
  READY: { backgroundColor: "#DBEAFE", color: "#1D4ED8" },
  IN_TRANSIT: { backgroundColor: "#FEF3C7", color: "#B45309" },
  ARRIVED: { backgroundColor: "#F3E8FF", color: "#7E22CE" },
  ARRIVED_AT_DESTINATION: { backgroundColor: "#F3E8FF", color: "#7E22CE" },
  OFFLOADING: { backgroundColor: "#FCE7F3", color: "#BE185D" },
  DELIVERED: { backgroundColor: "#D1FAE5", color: "#047857" },
  CANCELLED: { backgroundColor: "#FEE2E2", color: "#B91C1C" },
};

export function statusLabel(status) {
  return String(status || "Unknown")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export default function StatusBadge({ status }) {
  const normalizedStatus = String(status || "UNKNOWN").toUpperCase();
  const palette = statusStyles[normalizedStatus] || { backgroundColor: "#E5E7EB", color: "#4B5563" };

  return (
    <View style={[styles.badge, { backgroundColor: palette.backgroundColor }]}>
      <Text numberOfLines={1} style={[styles.label, { color: palette.color }]}>{statusLabel(normalizedStatus)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { borderRadius: 999, maxWidth: "52%", paddingHorizontal: 10, paddingVertical: 6 },
  label: { fontSize: 10, fontWeight: "800", letterSpacing: 0.2 },
});
