import { Ionicons } from "@expo/vector-icons";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { radius, shadow, spacing } from "../../constants/theme";

export function LoadingState({ label = "Loading…" }) {
  return <View style={styles.center}><ActivityIndicator color={colors.primary} size="large" /><Text style={styles.copy}>{label}</Text></View>;
}

export function EmptyState({ icon = "sparkles-outline", title, message, actionLabel, onAction }) {
  return <View style={styles.center}><View style={styles.iconCircle}><Ionicons color={colors.primary} name={icon} size={31} /></View><Text style={styles.title}>{title}</Text><Text style={styles.copy}>{message}</Text>{onAction ? <Pressable onPress={onAction} style={styles.action}><Text style={styles.actionText}>{actionLabel || "Try again"}</Text></Pressable> : null}</View>;
}

export function ErrorState({ message, onRetry, title = "Something went wrong" }) {
  return <EmptyState actionLabel="Try again" icon="cloud-offline-outline" message={message} onAction={onRetry} title={title} />;
}

const styles = StyleSheet.create({
  action: { backgroundColor: colors.primary, borderRadius: radius.md, marginTop: spacing.xl, paddingHorizontal: spacing.xl, paddingVertical: spacing.md },
  actionText: { color: colors.white, fontSize: 14, fontWeight: "700" },
  center: { alignItems: "center", flex: 1, justifyContent: "center", padding: spacing.xxxl },
  copy: { color: colors.gray, fontSize: 14, lineHeight: 21, marginTop: spacing.sm, maxWidth: 280, textAlign: "center" },
  iconCircle: { alignItems: "center", backgroundColor: "#DBEAFE", borderRadius: radius.pill, height: 74, justifyContent: "center", width: 74, ...shadow.card },
  title: { color: colors.text, fontSize: 19, fontWeight: "800", marginTop: spacing.lg, textAlign: "center" },
});
