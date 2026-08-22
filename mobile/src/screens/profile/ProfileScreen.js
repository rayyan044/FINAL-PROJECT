import { Ionicons } from "@expo/vector-icons";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, View } from "react-native";

import BottomNavigation from "../../components/navigation/BottomNavigation";
import ProfileInfoRow from "../../components/profile/ProfileInfoRow";
import { ErrorState, LoadingState } from "../../components/ui/ScreenState";
import colors from "../../constants/colors";
import { radius, shadow, spacing } from "../../constants/theme";
import { useAuth } from "../../context/AuthContext";
import { getDriverProfile } from "../../services/authService";
import StatusBadge, { statusLabel } from "../../components/deliveries/StatusBadge";

function displayName(profile) { return [profile?.firstName, profile?.lastName].filter(Boolean).join(" ") || profile?.username || "Driver"; }
function initials(profile) { return displayName(profile).split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase(); }

export default function ProfileScreen({ navigation }) {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [loggingOut, setLoggingOut] = useState(false);
  const { logout } = useAuth();

  const load = useCallback(async () => { try { setError(""); setProfile(await getDriverProfile()); } catch (loadError) { setError(loadError.message || "Unable to load your profile."); } }, []);
  useEffect(() => { load(); }, [load]);

  const handleLogout = async () => { try { setLoggingOut(true); await logout(); } catch (logoutError) { setError(logoutError.message || "Logout failed. Please try again."); } finally { setLoggingOut(false); } };

  if (!profile && !error) return <SafeAreaView style={styles.safeArea}><LoadingState label="Loading your profile…" /></SafeAreaView>;
  if (error && !profile) return <SafeAreaView style={styles.safeArea}><ErrorState message={error} onRetry={load} title="Profile unavailable" /></SafeAreaView>;

  const vehicle = profile.assignedVehicle;
  return <SafeAreaView style={styles.safeArea}><ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
    <View style={styles.hero}><View style={styles.heroGlow} /><View style={styles.avatar}><Text style={styles.avatarText}>{initials(profile)}</Text></View><Text style={styles.name}>{displayName(profile)}</Text><View style={styles.role}><View style={styles.availabilityDot} /><Text style={styles.roleText}>Driver • {statusLabel(profile.driverStatus || "Available")}</Text></View></View>
    <View style={styles.card}><Text style={styles.cardTitle}>Driver information</Text><ProfileInfoRow icon="person-outline" label="Username" value={profile.username} /><ProfileInfoRow icon="mail-outline" label="Email" value={profile.email} /><ProfileInfoRow icon="call-outline" label="Phone" value={profile.phone} /><ProfileInfoRow icon="id-card-outline" label="License number" last value={profile.licenseNumber} /></View>
    <View style={styles.card}><View style={styles.vehicleTitleRow}><View style={styles.vehicleIcon}><Ionicons color={colors.primary} name="bus-outline" size={21} /></View><Text style={styles.cardTitle}>Assigned vehicle</Text></View>{vehicle ? <><VehicleRow label="Truck number" value={vehicle.truckNumber} /><VehicleRow label="Plate number" value={vehicle.plateNumber} /><View style={styles.vehicleRow}><Text style={styles.vehicleLabel}>Vehicle status</Text><StatusBadge status={vehicle.status} /></View></> : <Text style={styles.emptyVehicle}>No vehicle is currently assigned.</Text>}</View>
    {error ? <Text style={styles.inlineError}>{error}</Text> : null}
    <Pressable accessibilityRole="button" disabled={loggingOut} onPress={handleLogout} style={({ pressed }) => [styles.logout, loggingOut && styles.disabled, pressed && styles.pressed]}>{loggingOut ? <ActivityIndicator color={colors.white} /> : <><Ionicons color={colors.white} name="log-out-outline" size={20} /><Text style={styles.logoutText}>Log Out</Text></>}</Pressable>
  </ScrollView><BottomNavigation activeRoute="Profile" navigation={navigation} /></SafeAreaView>;
}

function VehicleRow({ label, value }) { return <View style={styles.vehicleRow}><Text style={styles.vehicleLabel}>{label}</Text><Text style={styles.vehicleValue}>{value || "—"}</Text></View>; }

const styles = StyleSheet.create({
  availabilityDot: { backgroundColor: colors.success, borderColor: colors.white, borderRadius: 6, borderWidth: 2, height: 12, width: 12 }, avatar: { alignItems: "center", backgroundColor: "rgba(255,255,255,0.18)", borderColor: "rgba(255,255,255,0.28)", borderRadius: radius.pill, borderWidth: 2, height: 82, justifyContent: "center", width: 82 }, avatarText: { color: colors.white, fontSize: 27, fontWeight: "800" }, card: { backgroundColor: colors.white, borderColor: colors.border, borderRadius: radius.lg, borderWidth: 1, marginTop: spacing.lg, padding: spacing.lg, ...shadow.card }, cardTitle: { color: colors.text, fontSize: 16, fontWeight: "800" }, content: { paddingBottom: 112 }, disabled: { opacity: 0.7 }, emptyVehicle: { color: colors.gray, fontSize: 14, lineHeight: 21, marginTop: spacing.md }, hero: { alignItems: "center", backgroundColor: colors.primary, minHeight: 250, overflow: "hidden", paddingTop: spacing.xxxl }, heroGlow: { backgroundColor: colors.brightBlue, borderRadius: 150, height: 280, opacity: 0.35, position: "absolute", right: -110, top: -140, width: 280 }, inlineError: { color: colors.danger, fontSize: 13, marginHorizontal: spacing.xl, marginTop: spacing.md, textAlign: "center" }, logout: { alignItems: "center", backgroundColor: colors.danger, borderRadius: radius.md, flexDirection: "row", justifyContent: "center", marginHorizontal: spacing.xl, marginTop: spacing.xl, minHeight: 54, ...shadow.card }, logoutText: { color: colors.white, fontSize: 16, fontWeight: "800", marginLeft: spacing.sm }, name: { color: colors.white, fontSize: 25, fontWeight: "800", marginTop: spacing.md }, pressed: { opacity: 0.84 }, role: { alignItems: "center", flexDirection: "row", marginTop: spacing.sm }, roleText: { color: "#DBEAFE", fontSize: 13, fontWeight: "700", marginLeft: 6 }, safeArea: { backgroundColor: colors.background, flex: 1 }, vehicleIcon: { alignItems: "center", backgroundColor: "#EFF6FF", borderRadius: 12, height: 38, justifyContent: "center", marginRight: spacing.sm, width: 38 }, vehicleLabel: { color: colors.gray, fontSize: 13, fontWeight: "600" }, vehicleRow: { alignItems: "center", borderBottomColor: colors.border, borderBottomWidth: 1, flexDirection: "row", justifyContent: "space-between", paddingVertical: spacing.md }, vehicleTitleRow: { alignItems: "center", flexDirection: "row", marginBottom: spacing.sm }, vehicleValue: { color: colors.text, fontSize: 14, fontWeight: "800", maxWidth: "55%", textAlign: "right" },
});
