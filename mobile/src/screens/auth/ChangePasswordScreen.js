import { Ionicons } from "@expo/vector-icons";
import { useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import CustomButton from "../../components/CustomButton";
import CustomInput from "../../components/CustomInput";
import colors from "../../constants/colors";
import { useAuth } from "../../context/AuthContext";

export default function ChangePasswordScreen() {
  const { changePassword } = useAuth();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const submit = async () => {
    setError("");
    if (password.length < 6) {
      setError("Your new password must have at least 6 characters.");
      return;
    }
    if (password !== confirmPassword) {
      setError("The passwords do not match.");
      return;
    }

    setSaving(true);
    try {
      await changePassword(password, confirmPassword);
    } catch (changeError) {
      setError(changeError.message || "Unable to update your password. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <View style={styles.hero}>
            <View style={styles.icon}><Ionicons name="shield-checkmark-outline" size={34} color={colors.accent} /></View>
            <Text style={styles.title}>Secure your account</Text>
            <Text style={styles.subtitle}>Set a personal password before entering your driver workspace.</Text>
          </View>
          <View style={styles.card}>
            <Text style={styles.heading}>Change temporary password</Text>
            <Text style={styles.helper}>This replaces the password provided by Falcon Energy.</Text>
            <View style={styles.form}>
              <CustomInput
                autoCapitalize="none"
                autoComplete="new-password"
                icon="lock-closed-outline"
                isPassword
                label="New password"
                onChangeText={setPassword}
                onTogglePassword={() => setShowPassword((visible) => !visible)}
                passwordVisible={showPassword}
                placeholder="At least 6 characters"
                value={password}
              />
              <CustomInput
                autoCapitalize="none"
                autoComplete="new-password"
                icon="lock-closed-outline"
                isPassword
                label="Confirm new password"
                onChangeText={setConfirmPassword}
                onTogglePassword={() => setShowConfirmPassword((visible) => !visible)}
                passwordVisible={showConfirmPassword}
                placeholder="Enter the same password again"
                value={confirmPassword}
              />
              {error ? <View accessibilityLiveRegion="polite" style={styles.errorBanner}><Ionicons color={colors.danger} name="alert-circle-outline" size={18} /><Text style={styles.errorText}>{error}</Text></View> : null}
              <CustomButton loading={saving} onPress={submit} title="Save password and continue" />
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  scrollContent: { flexGrow: 1, paddingBottom: 32 },
  hero: { alignItems: "center", backgroundColor: colors.primary, paddingHorizontal: 28, paddingVertical: 54 },
  icon: { alignItems: "center", backgroundColor: "rgba(255,255,255,0.14)", borderRadius: 22, height: 64, justifyContent: "center", marginBottom: 16, width: 64 },
  title: { color: colors.white, fontSize: 27, fontWeight: "700", textAlign: "center" },
  subtitle: { color: "#DBEAFE", fontSize: 15, lineHeight: 22, marginTop: 10, maxWidth: 330, textAlign: "center" },
  card: { backgroundColor: colors.white, borderColor: colors.border, borderRadius: 22, borderWidth: 1, marginHorizontal: 20, marginTop: -26, padding: 24, shadowColor: "#0F172A", shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.08, shadowRadius: 18, elevation: 4 },
  heading: { color: colors.text, fontSize: 22, fontWeight: "700" },
  helper: { color: colors.gray, fontSize: 14, lineHeight: 20, marginTop: 6 },
  form: { marginTop: 24 },
  errorBanner: { alignItems: "flex-start", backgroundColor: "#FEF2F2", borderColor: "#FECACA", borderRadius: 12, borderWidth: 1, flexDirection: "row", marginBottom: 16, padding: 12 },
  errorText: { color: colors.danger, flex: 1, fontSize: 13, lineHeight: 18, marginLeft: 8 },
});
