import { useState } from "react";
import { SafeAreaView, StyleSheet, Text, View } from "react-native";

import CustomButton from "../../components/CustomButton";
import colors from "../../constants/colors";
import { useAuth } from "../../context/AuthContext";

export default function AuthenticatedScreen({ route }) {
  const { logout, user } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await logout();
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>{route?.name || "Dashboard"}</Text>
        <Text style={styles.message}>
          {user?.username ? `Welcome, ${user.username}.` : "Your session is active."}
        </Text>
        <View style={styles.buttonWrapper}>
          <CustomButton loading={isLoggingOut} onPress={handleLogout} title="Log out" />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.background,
    flex: 1,
  },
  content: {
    flex: 1,
    justifyContent: "center",
    padding: 24,
  },
  title: {
    color: colors.primary,
    fontSize: 28,
    fontWeight: "700",
    textAlign: "center",
  },
  message: {
    color: colors.gray,
    fontSize: 16,
    marginTop: 12,
    textAlign: "center",
  },
  buttonWrapper: {
    marginTop: 32,
  },
});
