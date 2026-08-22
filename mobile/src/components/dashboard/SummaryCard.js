import { StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { radius, shadow, spacing } from "../../constants/theme";

export default function SummaryCard({ icon, label, value, tone = colors.primary }) {
  return (
    <View style={styles.card}>
      <View style={[styles.iconCircle, { backgroundColor: `${tone}18` }]}>{icon}</View>
      <Text style={styles.value}>{value}</Text>
      <Text style={styles.label}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.white,
    borderColor: colors.border,
    borderRadius: radius.lg,
    borderWidth: 1,
    elevation: 2,
    flexBasis: "47.5%",
    minHeight: 132,
    padding: spacing.lg,
    ...shadow.card,
  },
  iconCircle: {
    alignItems: "center",
    borderRadius: 18,
    height: 36,
    justifyContent: "center",
    marginBottom: spacing.md,
    width: 36,
  },
  label: {
    color: colors.gray,
    fontSize: 13,
    lineHeight: 18,
    marginTop: 4,
  },
  value: {
    color: colors.text,
    fontSize: 27,
    fontWeight: "700",
  },
});
