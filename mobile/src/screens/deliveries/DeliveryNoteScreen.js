import { Ionicons } from "@expo/vector-icons";
import * as FileSystem from "expo-file-system/legacy";
import * as Sharing from "expo-sharing";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, View } from "react-native";

import { ErrorState, LoadingState } from "../../components/ui/ScreenState";
import colors from "../../constants/colors";
import { radius, shadow, spacing } from "../../constants/theme";
import { downloadDeliveryNotePdf, getDeliveryNote } from "../../services/deliveryService";

function dateTime(value) {
  const parsed = value ? new Date(value) : null;
  return parsed && !Number.isNaN(parsed.getTime()) ? parsed.toLocaleString() : "—";
}

function quantity(value) {
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toLocaleString()} L` : "—";
}

function base64FromArrayBuffer(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let index = 0; index < bytes.length; index += 1) binary += String.fromCharCode(bytes[index]);
  return globalThis.btoa(binary);
}

export default function DeliveryNoteScreen({ route }) {
  const deliveryId = route?.params?.deliveryId;
  const [note, setNote] = useState(null);
  const [error, setError] = useState("");
  const [downloading, setDownloading] = useState(false);

  const load = useCallback(async () => {
    try { setError(""); setNote(await getDeliveryNote(deliveryId)); }
    catch (loadError) { setError(loadError.message || "Unable to load the delivery note."); }
  }, [deliveryId]);

  useEffect(() => { load(); }, [load]);

  const handleDownload = async () => {
    try {
      setDownloading(true);
      const pdf = await downloadDeliveryNotePdf(deliveryId);
      const uri = `${FileSystem.cacheDirectory}${note.deliveryNoteNumber || `delivery-note-${deliveryId}`}.pdf`;
      await FileSystem.writeAsStringAsync(uri, base64FromArrayBuffer(pdf), { encoding: FileSystem.EncodingType.Base64 });
      if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(uri, { mimeType: "application/pdf", UTI: "com.adobe.pdf" });
      else setError("The PDF was saved on this device, but sharing is not available here.");
    } catch (downloadError) { setError(downloadError.message || "Unable to download the delivery note PDF."); }
    finally { setDownloading(false); }
  };

  if (!note && !error) return <SafeAreaView style={styles.safeArea}><LoadingState label="Loading delivery note…" /></SafeAreaView>;
  if (error && !note) return <SafeAreaView style={styles.safeArea}><ErrorState message={error} onRetry={load} title="Delivery Note Not Available" /></SafeAreaView>;

  return <SafeAreaView style={styles.safeArea}><ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
    <View style={styles.hero}><View style={styles.heroGlow} /><View style={styles.documentIcon}><Ionicons color={colors.white} name="document-text-outline" size={28} /></View><Text style={styles.heroLabel}>Customer Delivery Note</Text><Text style={styles.noteNumber}>{note.deliveryNoteNumber || "Delivery Note"}</Text><Text style={styles.date}>Generated {dateTime(note.preparedAt || note.createdAt)}</Text></View>
    <View style={styles.card}><Text style={styles.cardTitle}>Customer & delivery</Text><Detail icon="business-outline" label="Buyer / customer" value={note.customerName} /><Detail icon="location-outline" label="Destination" value={note.destination} /><Detail icon="cube-outline" label="Delivery reference" value={note.deliveryId ? `Delivery #${note.deliveryId}` : "—"} last /></View>
    <View style={styles.card}><Text style={styles.cardTitle}>Product details</Text><Detail icon="water-outline" label="Product" value={note.productName} /><Detail icon="speedometer-outline" label="Standard volume" value={quantity(note.standardVolume)} /><Detail icon="speedometer-outline" label="Ambient volume" value={quantity(note.ambientVolume)} last /></View>
    <View style={styles.card}><Text style={styles.cardTitle}>Vehicle & driver</Text><Detail icon="bus-outline" label="Truck number" value={note.truckNumber} /><Detail icon="person-outline" label="Driver" value={note.driverName} /><Detail icon="id-card-outline" label="Driver licence" value={note.driverLicenseNumber} last /></View>
    {error ? <Text style={styles.inlineError}>{error}</Text> : null}
    <Pressable accessibilityRole="button" disabled={downloading} onPress={handleDownload} style={({ pressed }) => [styles.download, downloading && styles.disabled, pressed && styles.pressed]}>{downloading ? <ActivityIndicator color={colors.white} /> : <><Ionicons color={colors.white} name="download-outline" size={20} /><Text style={styles.downloadText}>Download PDF</Text></>}</Pressable>
  </ScrollView></SafeAreaView>;
}

function Detail({ icon, label, value, last = false }) { return <View style={[styles.detail, !last && styles.divider]}><View style={styles.detailIcon}><Ionicons color={colors.primary} name={icon} size={18} /></View><View style={styles.detailCopy}><Text style={styles.detailLabel}>{label}</Text><Text style={styles.detailValue}>{value || "—"}</Text></View></View>; }

const styles = StyleSheet.create({
  card: { backgroundColor: colors.white, borderColor: colors.border, borderRadius: radius.lg, borderWidth: 1, marginHorizontal: spacing.lg, marginTop: spacing.lg, padding: spacing.lg, ...shadow.card }, cardTitle: { color: colors.text, fontSize: 16, fontWeight: "800", marginBottom: spacing.sm }, content: { paddingBottom: spacing.xxxl }, date: { color: "#DBEAFE", fontSize: 13, marginTop: spacing.sm }, detail: { alignItems: "center", flexDirection: "row", paddingVertical: spacing.md }, detailCopy: { flex: 1 }, detailIcon: { alignItems: "center", backgroundColor: "#EFF6FF", borderRadius: 12, height: 38, justifyContent: "center", marginRight: spacing.md, width: 38 }, detailLabel: { color: colors.gray, fontSize: 12, fontWeight: "600" }, detailValue: { color: colors.text, fontSize: 14, fontWeight: "700", marginTop: 2 }, disabled: { opacity: 0.7 }, divider: { borderBottomColor: colors.border, borderBottomWidth: 1 }, documentIcon: { alignItems: "center", backgroundColor: "rgba(255,255,255,0.16)", borderColor: "rgba(255,255,255,0.18)", borderRadius: 16, borderWidth: 1, height: 54, justifyContent: "center", width: 54 }, download: { alignItems: "center", backgroundColor: colors.primary, borderRadius: radius.md, flexDirection: "row", justifyContent: "center", marginHorizontal: spacing.lg, marginTop: spacing.xl, minHeight: 54, ...shadow.card }, downloadText: { color: colors.white, fontSize: 16, fontWeight: "800", marginLeft: spacing.sm }, hero: { backgroundColor: colors.primary, minHeight: 220, overflow: "hidden", padding: spacing.xl }, heroGlow: { backgroundColor: colors.brightBlue, borderRadius: 140, height: 260, opacity: 0.4, position: "absolute", right: -100, top: -130, width: 260 }, heroLabel: { color: "#DBEAFE", fontSize: 13, fontWeight: "700", marginTop: spacing.lg }, inlineError: { color: colors.danger, fontSize: 13, marginHorizontal: spacing.xl, marginTop: spacing.md, textAlign: "center" }, noteNumber: { color: colors.white, fontSize: 25, fontWeight: "800", marginTop: spacing.xs }, pressed: { opacity: 0.85 }, safeArea: { backgroundColor: colors.background, flex: 1 },
});
