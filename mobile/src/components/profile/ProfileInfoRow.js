import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { spacing } from "../../constants/theme";

export default function ProfileInfoRow({ icon, label, value, last = false }) {
  return <View style={[styles.row, !last && styles.withDivider]}><View style={styles.icon}><Ionicons color={colors.primary} name={icon} size={19} /></View><View style={styles.copy}><Text style={styles.label}>{label}</Text><Text numberOfLines={1} style={styles.value}>{value || "—"}</Text></View></View>;
}

const styles = StyleSheet.create({
  copy: { flex: 1 },
  icon: { alignItems: "center", backgroundColor: "#EFF6FF", borderRadius: 12, height: 38, justifyContent: "center", marginRight: spacing.md, width: 38 },
  label: { color: colors.gray, fontSize: 12, fontWeight: "600" },
  row: { alignItems: "center", flexDirection: "row", paddingVertical: spacing.md },
  value: { color: colors.text, fontSize: 14, fontWeight: "600", marginTop: 2 },
  withDivider: { borderBottomColor: colors.border, borderBottomWidth: 1 },
});
