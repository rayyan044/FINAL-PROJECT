import { Ionicons } from "@expo/vector-icons";
import { useRef, useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";

import CustomButton from "../../components/CustomButton";
import CustomInput from "../../components/CustomInput";
import colors from "../../constants/colors";
import { useAuth } from "../../context/AuthContext";

export default function LoginScreen() {
  const { login } = useAuth();
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const loginInProgress = useRef(false);

  const handleLogin = async () => {
    const trimmedUsername = username.trim();

    if (!trimmedUsername || !password) {
      setError("Enter your username and password.");
      return;
    }

    if (loginInProgress.current) {
      return;
    }

    setError("");
    loginInProgress.current = true;
    setIsLoading(true);

    try {
      await login(trimmedUsername, password);
    } catch (loginError) {
      setError(loginError.message || "Something went wrong. Try again.");
    } finally {
      loginInProgress.current = false;
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={styles.flex}
      >
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.brandSection}>
            <View style={styles.logoPlaceholder} accessibilityLabel="Falcon Energy logo placeholder">
              <Ionicons name="flash" size={34} color={colors.accent} />
            </View>
            <Text style={styles.title}>Falcon Energy</Text>
            <Text style={styles.subtitle}>Fuel Distribution Management System</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.welcomeText}>Welcome back</Text>
            <Text style={styles.helperText}>Sign in to access your driver workspace.</Text>

            <View style={styles.form}>
              <CustomInput
                autoCapitalize="none"
                autoComplete="username"
                icon="person-outline"
                label="Username"
                onChangeText={setUsername}
                placeholder="Enter your username"
                returnKeyType="next"
                value={username}
              />
              <CustomInput
                autoCapitalize="none"
                autoComplete="current-password"
                icon="lock-closed-outline"
                isPassword
                label="Password"
                onTogglePassword={() => setIsPasswordVisible((visible) => !visible)}
                onChangeText={setPassword}
                passwordVisible={isPasswordVisible}
                placeholder="Enter your password"
                returnKeyType="done"
                value={password}
              />

              <View accessibilityLiveRegion="polite" style={styles.errorArea}>
                {error ? <Text style={styles.errorText}>{error}</Text> : null}
              </View>

              <CustomButton loading={isLoading} onPress={handleLogin} title="Log in" />
            </View>
          </View>

          <Text style={styles.footer}>Falcon Energy • Driver Portal</Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  flex: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingVertical: 32,
  },
  brandSection: {
    alignItems: "center",
    marginBottom: 30,
  },
  logoPlaceholder: {
    alignItems: "center",
    backgroundColor: colors.primary,
    borderRadius: 22,
    height: 64,
    justifyContent: "center",
    marginBottom: 16,
    width: 64,
  },
  title: {
    color: colors.primary,
    fontSize: 29,
    fontWeight: "bold",
    letterSpacing: -0.5,
    marginBottom: 8,
  },
  subtitle: {
    color: colors.gray,
    fontSize: 15,
    lineHeight: 22,
    textAlign: "center",
  },
  card: {
    backgroundColor: colors.white,
    borderColor: colors.border,
    borderRadius: 20,
    borderWidth: 1,
    padding: 24,
    shadowColor: "#0F172A",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 18,
    elevation: 4,
  },
  welcomeText: {
    color: colors.text,
    fontSize: 23,
    fontWeight: "700",
    marginBottom: 6,
  },
  helperText: {
    color: colors.gray,
    fontSize: 14,
    lineHeight: 20,
  },
  form: {
    marginTop: 26,
  },
  errorArea: {
    minHeight: 30,
  },
  errorText: {
    color: colors.danger,
    fontSize: 13,
    lineHeight: 18,
    marginBottom: 10,
  },
  footer: {
    color: colors.gray,
    fontSize: 12,
    marginTop: 24,
    textAlign: "center",
  },
});
