import AsyncStorage from "@react-native-async-storage/async-storage";
import * as Location from "expo-location";
import * as TaskManager from "expo-task-manager";

import api from "../api/api";
import { AUTH_TOKEN_KEY } from "./authService";
import { updateLiveLocation } from "./deliveryService";

const LIVE_LOCATION_TASK = "falcon-live-delivery-location";
const LIVE_DELIVERY_KEY = "falcon_live_delivery";
let foregroundSubscription = null;

async function savedDelivery() {
  const raw = await AsyncStorage.getItem(LIVE_DELIVERY_KEY);
  return raw ? JSON.parse(raw) : null;
}

async function sendBackgroundLocation(location) {
  const tracking = await savedDelivery();
  const token = await AsyncStorage.getItem(AUTH_TOKEN_KEY);
  if (!tracking?.deliveryId || !token || !location?.coords) return;
  await api.post(
    `/mobile/deliveries/${tracking.deliveryId}/location`,
    {
      latitude: location.coords.latitude,
      longitude: location.coords.longitude,
      accuracy: location.coords.accuracy,
    },
    { headers: { Authorization: `Bearer ${token}` } },
  );
}

if (!TaskManager.isTaskDefined(LIVE_LOCATION_TASK)) {
  TaskManager.defineTask(LIVE_LOCATION_TASK, async ({ data, error }) => {
    if (error) return;
    const latestLocation = data?.locations?.[data.locations.length - 1];
    try {
      await sendBackgroundLocation(latestLocation);
    } catch {
      // The next scheduled update retries automatically. Do not crash the
      // location task when the phone temporarily loses its connection.
    }
  });
}

const trackingOptions = {
  accuracy: Location.Accuracy.Balanced,
  timeInterval: 30_000,
  distanceInterval: 75,
  deferredUpdatesInterval: 30_000,
  deferredUpdatesDistance: 75,
  foregroundService: {
    notificationTitle: "Falcon delivery tracking is active",
    notificationBody: "Your location is being shared with the customer during this delivery.",
  },
};

export async function startLiveTracking(deliveryId, initialLocation) {
  await stopLiveTracking();
  await AsyncStorage.setItem(LIVE_DELIVERY_KEY, JSON.stringify({ deliveryId }));

  const sendForegroundLocation = async (location) => {
    if (!location?.coords) return;
    try {
      await updateLiveLocation(
        deliveryId,
        location.coords.latitude,
        location.coords.longitude,
        location.coords.accuracy,
      );
    } catch {
      // A later GPS update will retry; the active trip remains usable.
    }
  };

  await sendForegroundLocation(initialLocation);
  foregroundSubscription = await Location.watchPositionAsync(trackingOptions, sendForegroundLocation);

  let backgroundEnabled = false;
  try {
    const backgroundPermission = await Location.requestBackgroundPermissionsAsync();
    if (backgroundPermission.status === "granted") {
      const running = await Location.hasStartedLocationUpdatesAsync(LIVE_LOCATION_TASK);
      if (!running) await Location.startLocationUpdatesAsync(LIVE_LOCATION_TASK, trackingOptions);
      backgroundEnabled = true;
    }
  } catch {
    // Foreground tracking is still active if the platform cannot grant
    // background permission (for example, Expo Go on iOS).
  }
  return { backgroundEnabled };
}

export async function stopLiveTracking() {
  foregroundSubscription?.remove();
  foregroundSubscription = null;
  try {
    if (await Location.hasStartedLocationUpdatesAsync(LIVE_LOCATION_TASK)) {
      await Location.stopLocationUpdatesAsync(LIVE_LOCATION_TASK);
    }
  } catch {
    // The task may not be available in a browser or in an Expo Go session.
  }
  await AsyncStorage.removeItem(LIVE_DELIVERY_KEY);
}
