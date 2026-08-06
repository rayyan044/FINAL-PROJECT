import { ActivityIndicator, Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";
import { useCallback, useEffect, useState } from "react";

import colors from "../../constants/colors";
import { getMyDelivery } from "../../services/deliveryService";

export default function DeliveryDetailsScreen({ route }) {
  const [delivery, setDelivery] = useState(null);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    try {
      setError("");
      setDelivery(await getMyDelivery(route?.params?.deliveryId));
    } catch (loadError) {
      setError(loadError.message || "Unable to load delivery details.");
    }
  }, [route?.params?.deliveryId]);
  useEffect(() => { load(); }, [load]);
  if (!delivery && !error) return <SafeAreaView style={styles.safeArea}><View style={styles.content}><ActivityIndicator color={colors.primary} size="large" /></View></SafeAreaView>;
  if (error) return <SafeAreaView style={styles.safeArea}><View style={styles.content}><Text style={styles.title}>Delivery unavailable</Text><Text style={styles.message}>{error}</Text><Pressable onPress={load} style={styles.retry}><Text style={styles.retryText}>Retry</Text></Pressable></View></SafeAreaView>;
  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.content}>
        <Text style={styles.title}>{delivery.deliveryNoteNumber || "Delivery Details"}</Text>
        <Text style={styles.detail}>{delivery.customerName || "Customer unavailable"}</Text>
        <Text style={styles.detail}>{delivery.fuelProduct || "Product unavailable"} · {delivery.quantity || "—"}</Text>
        <Text style={styles.detail}>{delivery.destination || "Destination unavailable"}</Text>
        <Text style={styles.status}>{delivery.currentStatus || "UNKNOWN"}</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  content: { alignItems: "center", flex: 1, justifyContent: "center", padding: 24 },
  message: { color: colors.gray, fontSize: 15, marginTop: 10, textAlign: "center" },
  detail: { color: colors.gray, fontSize: 15, marginTop: 10, textAlign: "center" },
  retry: { backgroundColor: colors.primary, borderRadius: 10, marginTop: 20, paddingHorizontal: 22, paddingVertical: 12 },
  retryText: { color: colors.white, fontWeight: "700" },
  status: { color: colors.primary, fontSize: 15, fontWeight: "700", marginTop: 18 },
  safeArea: { backgroundColor: colors.background, flex: 1 },
  title: { color: colors.text, fontSize: 24, fontWeight: "700", marginTop: 16 },
});
