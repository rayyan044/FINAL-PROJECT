import { Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";

function formatQuantity(quantity) {
  const value = Number(quantity);
  return Number.isFinite(value) ? `${value.toLocaleString()} L` : "Quantity unavailable";
}

export default function DeliveryCard({ delivery, onOpen }) {
  return (
    <View style={styles.card}>
      <View style={styles.topRow}>
        <Text numberOfLines={1} style={styles.note}>
          {delivery.deliveryNoteNumber || "Delivery note pending"}
        </Text>
        <View style={styles.status}>
          <Text numberOfLines={1} style={styles.statusText}>
            {delivery.currentStatus || "Unknown"}
          </Text>
        </View>
      </View>
      <Text numberOfLines={1} style={styles.customer}>
        {delivery.customerName || "Customer unavailable"}
      </Text>
      <Text numberOfLines={1} style={styles.detail}>
        {delivery.fuelProduct || "Product unavailable"} · {formatQuantity(delivery.quantity)}
      </Text>
      <Text numberOfLines={1} style={styles.destination}>
        {delivery.destination || "Destination unavailable"}
      </Text>
      <Pressable accessibilityRole="button" onPress={onOpen} style={({ pressed }) => [styles.openButton, pressed && styles.pressed]}>
        <Text style={styles.openText}>Open</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: colors.white, borderColor: colors.border, borderRadius: 16, borderWidth: 1, marginBottom: 12, padding: 17 },
  customer: { color: colors.text, fontSize: 16, fontWeight: "700", marginTop: 13 },
  destination: { color: colors.gray, fontSize: 13, marginTop: 9 },
  detail: { color: colors.gray, fontSize: 13, marginTop: 5 },
  note: { color: colors.primary, flex: 1, fontSize: 13, fontWeight: "700", marginRight: 10 },
  openButton: { alignSelf: "flex-start", backgroundColor: "#EFF6FF", borderRadius: 8, marginTop: 15, paddingHorizontal: 17, paddingVertical: 8 },
  openText: { color: colors.primary, fontSize: 13, fontWeight: "700" },
  pressed: { opacity: 0.7 },
  status: { backgroundColor: "#FEF3C7", borderRadius: 6, maxWidth: "45%", paddingHorizontal: 8, paddingVertical: 4 },
  statusText: { color: "#92400E", fontSize: 10, fontWeight: "700" },
  topRow: { alignItems: "center", flexDirection: "row" },
});
