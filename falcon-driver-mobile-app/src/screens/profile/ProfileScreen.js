import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { getDriverProfile } from "../../services/authService";
import { useAuth } from "../../context/AuthContext";

export default function ProfileScreen() {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const { logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);

  const load = useCallback(async () => {
    try {
      setError("");
      setProfile(await getDriverProfile());
    } catch (loadError) {
      setError(loadError.message || "Unable to load your profile.");
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleLogout = async () => {
    try {
      setLoggingOut(true);
      await logout();
    } catch (err) {
      setError(err.message || "Logout failed. Please try again.");
    } finally {
      setLoggingOut(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.content}>
        {!profile && !error ? <ActivityIndicator color={colors.primary} size="large" /> : null}
        {error ? <><Text style={styles.title}>Profile unavailable</Text><Text style={styles.message}>{error}</Text><Pressable onPress={load} style={styles.retry}><Text style={styles.retryText}>Retry</Text></Pressable></> : null}
        {profile ? <>
          <Text style={styles.title}>{[profile.firstName, profile.lastName].filter(Boolean).join(" ") || profile.username}</Text>
          <Text style={styles.detail}>Username: {profile.username || "—"}</Text>
          <Text style={styles.detail}>Email: {profile.email || "—"}</Text>
          <Text style={styles.detail}>Phone: {profile.phone || "—"}</Text>
          <Text style={styles.detail}>License Number: {profile.licenseNumber || "—"}</Text>

          <View style={styles.divider} />

          {profile.assignedVehicle ? (
            <View style={styles.vehicleContainer}>
              <Text style={styles.vehicleTitle}>Assigned Vehicle</Text>
              <Text style={styles.vehicleDetail}>Truck Number: {profile.assignedVehicle.truckNumber || "—"}</Text>
              <Text style={styles.vehicleDetail}>Plate Number: {profile.assignedVehicle.plateNumber || "—"}</Text>
              <Text style={styles.vehicleDetail}>Vehicle Status: {profile.assignedVehicle.status || "—"}</Text>
            </View>
          ) : (
            <Text style={styles.noVehicle}>No assigned vehicle</Text>
          )}

          <Text style={styles.status}>DRIVER · {profile.driverStatus || "—"}</Text>

          <Pressable 
            onPress={handleLogout} 
            style={[styles.logoutButton, loggingOut && styles.disabledButton]}
            disabled={loggingOut}
          >
            {loggingOut ? (
              <ActivityIndicator color={colors.white} size="small" />
            ) : (
              <Text style={styles.logoutText}>Log Out</Text>
            )}
          </Pressable>
        </> : null}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  content: { alignItems: "center", flex: 1, justifyContent: "center", padding: 24 },
  detail: { color: colors.gray, fontSize: 15, marginTop: 10, textAlign: "center" },
  divider: { backgroundColor: colors.border, height: 1, marginVertical: 20, width: "80%" },
  message: { color: colors.gray, fontSize: 15, marginTop: 10, textAlign: "center" },
  noVehicle: { color: colors.gray, fontSize: 15, fontStyle: "italic", marginTop: 10 },
  retry: { backgroundColor: colors.primary, borderRadius: 10, marginTop: 20, paddingHorizontal: 22, paddingVertical: 12 },
  retryText: { color: colors.white, fontWeight: "700" },
  safeArea: { backgroundColor: colors.background, flex: 1 },
  status: { color: colors.primary, fontSize: 15, fontWeight: "700", marginTop: 24 },
  title: { color: colors.text, fontSize: 24, fontWeight: "700", textAlign: "center" },
  vehicleContainer: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.border, borderRadius: 12, borderWidth: 1, padding: 16, width: "100%" },
  vehicleDetail: { color: colors.text, fontSize: 14, marginTop: 6, textAlign: "center" },
  vehicleTitle: { color: colors.primary, fontSize: 16, fontWeight: "700", marginBottom: 6 },
  logoutButton: { backgroundColor: colors.danger, borderRadius: 10, marginTop: 30, paddingHorizontal: 40, paddingVertical: 14, width: "100%", alignItems: "center" },
  logoutText: { color: colors.white, fontWeight: "700", fontSize: 16 },
  disabledButton: { opacity: 0.7 },
});
