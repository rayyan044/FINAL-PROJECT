import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { radius, shadow, spacing } from "../../constants/theme";
import StatusBadge from "./StatusBadge";

function formatQuantity(quantity) {
  const value = Number(quantity);
  return Number.isFinite(value) ? `${value.toLocaleString()} Litres` : "Quantity unavailable";
}

function formatDate(date) {
  const parsed = date ? new Date(date) : null;
  return parsed && !Number.isNaN(parsed.getTime())
    ? parsed.toLocaleDateString(undefined, { day: "numeric", month: "short" })
    : null;
}

export default function DeliveryCard({ delivery, onViewDetails }) {
  return (
    <View style={styles.card}>
      <View style={styles.topRow}>
        <Text numberOfLines={1} style={styles.note}>{delivery.deliveryNoteNumber || "Delivery note pending"}</Text>
        <StatusBadge status={delivery.currentStatus} />
      </View>
      <Text numberOfLines={2} style={styles.customer}>{delivery.customerName || "Customer unavailable"}</Text>
      <Text numberOfLines={1} style={styles.detail}>{delivery.fuelProduct || "Product unavailable"} <Text style={styles.dot}>•</Text> {formatQuantity(delivery.quantity)}</Text>
      <View style={styles.metaRow}><Ionicons color={colors.gray} name="location-outline" size={15} /><Text numberOfLines={1} style={styles.metaText}>{delivery.destination || "Destination unavailable"}</Text></View>
      {formatDate(delivery.scheduledDeliveryDate) ? <View style={styles.metaRow}><Ionicons color={colors.gray} name="calendar-outline" size={14} /><Text style={styles.metaText}>{formatDate(delivery.scheduledDeliveryDate)}</Text></View> : null}
      <Pressable accessibilityRole="button" onPress={onViewDetails} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
        <Text style={styles.buttonText}>View Details</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  button: { alignSelf: "flex-start", backgroundColor: "#EFF6FF", borderRadius: radius.sm, marginTop: spacing.lg, paddingHorizontal: 16, paddingVertical: 10 },
  buttonText: { color: colors.primary, fontSize: 13, fontWeight: "700" },
  card: { backgroundColor: colors.white, borderColor: colors.border, borderRadius: radius.lg, borderWidth: 1, marginBottom: spacing.md, padding: spacing.lg, ...shadow.card },
  customer: { color: colors.text, fontSize: 16, fontWeight: "800", lineHeight: 22, marginTop: spacing.md },
  detail: { color: colors.gray, fontSize: 13, lineHeight: 20, marginTop: spacing.sm },
  dot: { color: colors.muted },
  metaRow: { alignItems: "center", flexDirection: "row", marginTop: spacing.sm },
  metaText: { color: colors.gray, flex: 1, fontSize: 13, marginLeft: 6 },
  note: { color: colors.primary, flex: 1, fontSize: 13, fontWeight: "800", marginRight: 10 },
  pressed: { opacity: 0.7 },
  topRow: { alignItems: "center", flexDirection: "row" },
});
