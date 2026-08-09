import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { ActivityIndicator, StyleSheet, View } from "react-native";

import colors from "../constants/colors";
import { useAuth } from "../context/AuthContext";
import DashboardScreen from "../screens/dashboard/DashboardScreen";
import NotificationsScreen from "../screens/dashboard/NotificationsScreen";
import ProfileScreen from "../screens/profile/ProfileScreen";
import DeliveryDetailsScreen from "../screens/deliveries/DeliveryDetailsScreen";
import MyDeliveriesScreen from "../screens/deliveries/MyDeliveriesScreen";
import LoginScreen from "../screens/auth/LoginScreen";

const Stack = createNativeStackNavigator();

export default function AppNavigator() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
        }}
      >
        {isAuthenticated ? (
          <>
            <Stack.Screen name="Dashboard" component={DashboardScreen} />
            <Stack.Screen name="Deliveries" component={MyDeliveriesScreen} />
            <Stack.Screen name="Notifications" component={NotificationsScreen} options={{ headerShown: true, title: "Notifications" }} />
            <Stack.Screen name="Profile" component={ProfileScreen} options={{ headerShown: true, title: "Profile" }} />
            <Stack.Screen name="DeliveryDetails" component={DeliveryDetailsScreen} options={{ headerShown: true, title: "Delivery Details" }} />
          </>
        ) : (
          <Stack.Screen name="Login" component={LoginScreen} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    alignItems: "center",
    backgroundColor: colors.background,
    flex: 1,
    justifyContent: "center",
  },
});
