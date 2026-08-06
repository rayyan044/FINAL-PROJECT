import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import StatusBadge from "./StatusBadge";

function formatQuantity(quantity) {
  const value = Number(quantity);
  return Number.isFinite(value) ? `${value.toLocaleString()} Litres` : "Quantity unavailable";
}

function formatDate(date) {
  const parsed = date ? new Date(date) : null;
  return parsed && !Number.isNaN(parsed.getTime())
    ? parsed.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" })
    : "Not scheduled";
}

export default function DeliveryCard({ delivery, onViewDetails }) {
  return (
    <View style={styles.card}>
      <View style={styles.topRow}>
        <Text numberOfLines={1} style={styles.note}>{delivery.deliveryNoteNumber || "Delivery note pending"}</Text>
        <StatusBadge status={delivery.currentStatus} />
      </View>
      <Text numberOfLines={1} style={styles.customer}>{delivery.customerName || "Customer unavailable"}</Text>
      <Detail label="Fuel product" value={delivery.fuelProduct || "Product unavailable"} />
      <Detail label="Quantity" value={formatQuantity(delivery.quantity)} />
      <Detail label="Destination" value={delivery.destination || "Destination unavailable"} />
      <Detail label="Scheduled" value={formatDate(delivery.scheduledDeliveryDate)} />
      <Pressable accessibilityRole="button" onPress={onViewDetails} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
        <Text style={styles.buttonText}>View Details</Text>
      </Pressable>
    </View>
  );
}

function Detail({ label, value }) {
  return <Text numberOfLines={1} style={styles.detail}><Text style={styles.detailLabel}>{label}: </Text>{value}</Text>;
}

const styles = StyleSheet.create({
  button: { alignSelf: "flex-start", backgroundColor: "#EFF6FF", borderRadius: 8, marginTop: 16, paddingHorizontal: 16, paddingVertical: 10 },
  buttonText: { color: colors.primary, fontSize: 13, fontWeight: "700" },
  card: { backgroundColor: colors.white, borderColor: colors.border, borderRadius: 16, borderWidth: 1, marginBottom: 12, padding: 17 },
  customer: { color: colors.text, fontSize: 16, fontWeight: "700", marginBottom: 10, marginTop: 13 },
  detail: { color: colors.gray, fontSize: 13, lineHeight: 20, marginTop: 3 },
  detailLabel: { color: colors.text, fontWeight: "600" },
  note: { color: colors.primary, flex: 1, fontSize: 13, fontWeight: "800", marginRight: 10 },
  pressed: { opacity: 0.7 },
  topRow: { alignItems: "center", flexDirection: "row" },
});
