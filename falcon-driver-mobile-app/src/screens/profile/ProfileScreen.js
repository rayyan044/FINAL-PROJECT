import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";

import colors from "../../constants/colors";
import { getCurrentProfile } from "../../services/authService";

export default function ProfileScreen() {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    try {
      setError("");
      setProfile(await getCurrentProfile());
    } catch (loadError) {
      setError(loadError.message || "Unable to load your profile.");
    }
  }, []);

  useEffect(() => { load(); }, [load]);

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
          <Text style={styles.status}>{profile.role || "—"} · {profile.status || "—"}</Text>
        </> : null}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  content: { alignItems: "center", flex: 1, justifyContent: "center", padding: 24 },
  detail: { color: colors.gray, fontSize: 15, marginTop: 10, textAlign: "center" },
  message: { color: colors.gray, fontSize: 15, marginTop: 10, textAlign: "center" },
  retry: { backgroundColor: colors.primary, borderRadius: 10, marginTop: 20, paddingHorizontal: 22, paddingVertical: 12 },
  retryText: { color: colors.white, fontWeight: "700" },
  safeArea: { backgroundColor: colors.background, flex: 1 },
  status: { color: colors.primary, fontSize: 15, fontWeight: "700", marginTop: 18 },
  title: { color: colors.text, fontSize: 24, fontWeight: "700", textAlign: "center" },
});
