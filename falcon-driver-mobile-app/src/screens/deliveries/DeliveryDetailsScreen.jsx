import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import * as Location from "expo-location";
import { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Image,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";

import colors from "../../constants/colors";
import {
  acceptDelivery,
  completeDelivery,
  getMyDelivery,
  markArrived,
  startTrip,
  uploadProofOfDelivery,
} from "../../services/deliveryService";

export default function DeliveryDetailsScreen({ route, navigation }) {
  const deliveryId = route?.params?.deliveryId;

  const [delivery, setDelivery] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Form states for Arrival
  const [receivedBy, setReceivedBy] = useState("");
  const [arrivalRemarks, setArrivalRemarks] = useState("");

  // Form states for POD Upload
  const [photoUri, setPhotoUri] = useState(null);
  const [podNotes, setPodNotes] = useState("");

  const load = useCallback(async () => {
    try {
      setError("");
      setLoading(true);
      const data = await getMyDelivery(deliveryId);
      setDelivery(data);
      if (data.receivedBy) setReceivedBy(data.receivedBy);
      if (data.remarks) setArrivalRemarks(data.remarks);
      if (data.podNotes) setPodNotes(data.podNotes);
      if (data.podPhotoPath) {
        // Build URL to retrieve POD photo securely
        // Using relative path to match API configuration
        setPhotoUri(`/mobile/deliveries/${deliveryId}/proof`);
      }
    } catch (loadError) {
      setError(loadError.message || "Unable to load delivery details.");
    } finally {
      setLoading(false);
    }
  }, [deliveryId]);

  useEffect(() => {
    load();
  }, [load]);

  const showAlert = (title, message) => {
    if (Platform.OS === "web") {
      window.alert(`${title}: ${message}`);
    } else {
      Alert.alert(title, message);
    }
  };

  const handleAccept = async () => {
    try {
      setSubmitting(true);
      await acceptDelivery(deliveryId);
      showAlert("Success", "Delivery accepted successfully.");
      await load();
    } catch (err) {
      showAlert("Error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleStartTrip = async () => {
    try {
      setSubmitting(true);

      // Request and capture GPS coordinates
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") {
        showAlert("Permission Denied", "Location permissions are required to start the trip.");
        setSubmitting(false);
        return;
      }

      const location = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });
      const { latitude, longitude } = location.coords;

      await startTrip(deliveryId, latitude, longitude);
      showAlert("Trip Started", "Location logged and status updated to In Transit.");
      await load();
    } catch (err) {
      showAlert("Error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleArrive = async () => {
    try {
      setSubmitting(true);
      await markArrived(deliveryId, receivedBy, arrivalRemarks);
      showAlert("Arrived", "Arrival at destination logged.");
      await load();
    } catch (err) {
      showAlert("Error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCapturePhoto = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== "granted") {
      showAlert("Permission Denied", "Camera permissions are required to capture the proof of delivery.");
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 0.8,
    });

    if (!result.canceled && result.assets && result.assets.length > 0) {
      setPhotoUri(result.assets[0].uri);
    }
  };

  const handleUploadProof = async () => {
    if (!photoUri) {
      showAlert("Required", "Please capture a photo first.");
      return;
    }

    try {
      setSubmitting(true);

      // Capture GPS coordinates at upload time
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") {
        showAlert("Permission Denied", "Location permissions are required to complete Proof of Delivery.");
        setSubmitting(false);
        return;
      }

      const location = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });
      const { latitude, longitude } = location.coords;

      // Prepare file data
      let fileData;
      if (Platform.OS === "web") {
        const response = await fetch(photoUri);
        const blob = await response.blob();
        fileData = new File([blob], "pod.jpg", { type: "image/jpeg" });
      } else {
        fileData = {
          uri: photoUri,
          name: "pod.jpg",
          type: "image/jpeg",
        };
      }

      await uploadProofOfDelivery(deliveryId, fileData, latitude, longitude, podNotes);
      showAlert("Success", "Proof of delivery uploaded. Status is now Delivered.");
      await load();
    } catch (err) {
      showAlert("Error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleComplete = async () => {
    try {
      setSubmitting(true);
      await completeDelivery(deliveryId);
      showAlert("Success", "Delivery finalized and completed. Vehicle freed.");
      navigation.navigate("Dashboard");
    } catch (err) {
      showAlert("Error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centered}>
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.loadingText}>Loading delivery details...</Text>
        </View>
      </SafeAreaView>
    );
  }

  if (error) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centered}>
          <Ionicons color={colors.danger} name="alert-circle-outline" size={48} />
          <Text style={styles.errorTitle}>Error Loading Delivery</Text>
          <Text style={styles.errorMessage}>{error}</Text>
          <Pressable onPress={load} style={styles.retryButton}>
            <Text style={styles.retryText}>Retry</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

  const renderStatusBadge = (status) => {
    let color = colors.primary;
    if (status === "COMPLETED") color = colors.success;
    if (status === "ARRIVED_AT_DESTINATION" || status === "DELIVERED") color = colors.accent;
    if (status === "CANCELLED") color = colors.danger;

    return (
      <View style={[styles.badge, { backgroundColor: color }]}>
        <Text style={styles.badgeText}>{status}</Text>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.scrollContainer} showsVerticalScrollIndicator={false}>
        {/* Header Block */}
        <View style={styles.headerBlock}>
          <View style={styles.headerRow}>
            <Text style={styles.deliveryNumber}>{delivery.deliveryNoteNumber || "DEL-PENDING"}</Text>
            {renderStatusBadge(delivery.currentStatus)}
          </View>
          <Text style={styles.customerName}>{delivery.customerName || "Customer name unavailable"}</Text>
        </View>

        {/* Info Section */}
        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Product Details</Text>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Product:</Text>
            <Text style={styles.infoValue}>{delivery.fuelProduct || "—"}</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Allocated Volume:</Text>
            <Text style={styles.infoValue}>{delivery.quantity ? `${delivery.quantity} L` : "—"}</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Scheduled Date:</Text>
            <Text style={styles.infoValue}>
              {delivery.scheduledDeliveryDate ? new Date(delivery.scheduledDeliveryDate).toLocaleDateString() : "—"}
            </Text>
          </View>
        </View>

        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Route & Address</Text>
          <View style={styles.destinationRow}>
            <Ionicons color={colors.primary} name="location-outline" size={20} style={styles.locationIcon} />
            <Text style={styles.destinationText}>{delivery.destination || "—"}</Text>
          </View>
        </View>

        {/* Workflow State Action Controller */}
        <View style={styles.actionCard}>
          <Text style={styles.actionTitle}>Workflow Step</Text>

          {submitting ? (
            <ActivityIndicator color={colors.primary} size="small" style={styles.spinner} />
          ) : (
            <>
              {/* ASSIGNED STATE */}
              {delivery.currentStatus === "ASSIGNED" && (
                <View>
                  <Text style={styles.instructionText}>
                    Please review the shipment details and accept the delivery to proceed.
                  </Text>
                  <Pressable onPress={handleAccept} style={styles.actionButton}>
                    <Text style={styles.actionButtonText}>Accept Delivery</Text>
                  </Pressable>
                </View>
              )}

              {/* ACCEPTED STATE */}
              {delivery.currentStatus === "ACCEPTED" && (
                <View>
                  <Text style={styles.instructionText}>
                    Once the vehicle departs from the loading station, start the trip. This logs your starting GPS coordinates.
                  </Text>
                  <Pressable onPress={handleStartTrip} style={styles.actionButton}>
                    <Text style={styles.actionButtonText}>Start Trip</Text>
                  </Pressable>
                </View>
              )}

              {/* IN_TRANSIT STATE */}
              {delivery.currentStatus === "IN_TRANSIT" && (
                <View>
                  <Text style={styles.instructionText}>
                    You are in transit. Upon arrival at the customer destination, record details to mark arrival.
                  </Text>

                  <Text style={styles.inputLabel}>Received By</Text>
                  <TextInput
                    onChangeText={setReceivedBy}
                    placeholder="Enter customer contact name"
                    style={styles.inputField}
                    value={receivedBy}
                  />

                  <Text style={styles.inputLabel}>Arrival Remarks</Text>
                  <TextInput
                    multiline
                    numberOfLines={3}
                    onChangeText={setArrivalRemarks}
                    placeholder="Enter notes or remarks"
                    style={[styles.inputField, styles.textArea]}
                    value={arrivalRemarks}
                  />

                  <Pressable onPress={handleArrive} style={styles.actionButton}>
                    <Text style={styles.actionButtonText}>Arrived at Destination</Text>
                  </Pressable>
                </View>
              )}

              {/* ARRIVED_AT_DESTINATION STATE */}
              {delivery.currentStatus === "ARRIVED_AT_DESTINATION" && (
                <View>
                  <Text style={styles.instructionText}>
                    Capture a clear photo of the signed proof-of-delivery (POD) document to transition to Delivered.
                  </Text>

                  <Pressable onPress={handleCapturePhoto} style={styles.captureButton}>
                    <Ionicons color={colors.primary} name="camera" size={20} />
                    <Text style={styles.captureButtonText}>
                      {photoUri ? "Retake Document Photo" : "Capture Document Photo"}
                    </Text>
                  </Pressable>

                  {photoUri ? (
                    <View style={styles.imagePreviewContainer}>
                      {photoUri.startsWith("/") ? (
                        <Text style={styles.imageUploadedText}>Proof photo stored securely on server</Text>
                      ) : (
                        <Image source={{ uri: photoUri }} style={styles.previewImage} />
                      )}
                    </View>
                  ) : null}

                  <Text style={styles.inputLabel}>POD Notes</Text>
                  <TextInput
                    multiline
                    numberOfLines={3}
                    onChangeText={setPodNotes}
                    placeholder="Enter proof notes (e.g. seal numbers, differences in volume)"
                    style={[styles.inputField, styles.textArea]}
                    value={podNotes}
                  />

                  <Pressable onPress={handleUploadProof} style={styles.actionButton}>
                    <Text style={styles.actionButtonText}>Submit Proof of Delivery</Text>
                  </Pressable>
                </View>
              )}

              {/* DELIVERED STATE */}
              {delivery.currentStatus === "DELIVERED" && (
                <View>
                  <Text style={styles.instructionText}>
                    Proof of delivery has been submitted. Confirm and complete the trip to finalize the dispatch and free the vehicle.
                  </Text>

                  {delivery.podPhotoPath ? (
                    <View style={styles.podDetailCard}>
                      <Text style={styles.podTitle}>POD Uploaded</Text>
                      {delivery.podNotes ? <Text style={styles.podNotes}>Notes: {delivery.podNotes}</Text> : null}
                    </View>
                  ) : null}

                  <Pressable onPress={handleComplete} style={styles.actionButton}>
                    <Text style={styles.actionButtonText}>Complete Delivery</Text>
                  </Pressable>
                </View>
              )}

              {/* COMPLETED STATE */}
              {delivery.currentStatus === "COMPLETED" && (
                <View style={styles.completedState}>
                  <Ionicons color={colors.success} name="checkmark-done-circle" size={48} />
                  <Text style={styles.completedStateTitle}>Delivery Finalized</Text>
                  <Text style={styles.completedStateMessage}>
                    This delivery has been successfully executed, proof uploaded, and the dispatch closed.
                  </Text>
                  {delivery.podNotes ? (
                    <Text style={styles.completedRemarks}>Remarks: {delivery.remarks || delivery.podNotes}</Text>
                  ) : null}
                </View>
              )}

              {/* CANCELLED STATE */}
              {delivery.currentStatus === "CANCELLED" && (
                <View style={styles.cancelledState}>
                  <Ionicons color={colors.danger} name="close-circle" size={48} />
                  <Text style={styles.cancelledStateTitle}>Trip Cancelled</Text>
                  <Text style={styles.cancelledStateMessage}>
                    Remarks: {delivery.remarks || "No reason specified."}
                  </Text>
                </View>
              )}
            </>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  actionButton: {
    alignItems: "center",
    backgroundColor: colors.primary,
    borderRadius: 12,
    marginTop: 18,
    padding: 16,
  },
  actionButtonText: {
    color: colors.white,
    fontSize: 16,
    fontWeight: "700",
  },
  actionCard: {
    backgroundColor: colors.white,
    borderColor: colors.border,
    borderRadius: 16,
    borderWidth: 1,
    marginTop: 18,
    padding: 20,
  },
  actionTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: "700",
    marginBottom: 12,
  },
  badge: {
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  badgeText: {
    color: colors.white,
    fontSize: 12,
    fontWeight: "700",
  },
  cancelledState: {
    alignItems: "center",
    paddingVertical: 12,
  },
  cancelledStateMessage: {
    color: colors.gray,
    fontSize: 14,
    marginTop: 6,
    textAlign: "center",
  },
  cancelledStateTitle: {
    color: colors.danger,
    fontSize: 16,
    fontWeight: "700",
    marginTop: 10,
  },
  captureButton: {
    alignItems: "center",
    borderColor: colors.primary,
    borderRadius: 12,
    borderWidth: 1.5,
    flexDirection: "row",
    gap: 8,
    justifyContent: "center",
    marginTop: 10,
    padding: 14,
  },
  captureButtonText: {
    color: colors.primary,
    fontSize: 15,
    fontWeight: "700",
  },
  centered: {
    alignItems: "center",
    backgroundColor: colors.background,
    flex: 1,
    justifyContent: "center",
    padding: 24,
  },
  completedRemarks: {
    color: colors.gray,
    fontSize: 14,
    fontStyle: "italic",
    marginTop: 10,
    textAlign: "center",
  },
  completedState: {
    alignItems: "center",
    paddingVertical: 12,
  },
  completedStateMessage: {
    color: colors.gray,
    fontSize: 14,
    lineHeight: 20,
    marginTop: 6,
    textAlign: "center",
  },
  completedStateTitle: {
    color: colors.success,
    fontSize: 16,
    fontWeight: "700",
    marginTop: 10,
  },
  customerName: {
    color: colors.gray,
    fontSize: 16,
    marginTop: 8,
  },
  deliveryNumber: {
    color: colors.text,
    fontSize: 22,
    fontWeight: "800",
  },
  destinationRow: {
    flexDirection: "row",
    marginTop: 6,
  },
  destinationText: {
    color: colors.text,
    flex: 1,
    fontSize: 15,
    lineHeight: 22,
  },
  errorMessage: {
    color: colors.gray,
    fontSize: 14,
    marginTop: 8,
    textAlign: "center",
  },
  errorTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: "700",
    marginTop: 16,
  },
  headerBlock: {
    backgroundColor: colors.white,
    borderColor: colors.border,
    borderRadius: 16,
    borderWidth: 1,
    padding: 20,
  },
  headerRow: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
  },
  imagePreviewContainer: {
    alignItems: "center",
    marginTop: 14,
  },
  imageUploadedText: {
    color: colors.success,
    fontSize: 14,
    fontWeight: "600",
  },
  infoLabel: {
    color: colors.gray,
    fontSize: 14,
  },
  infoRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 10,
  },
  infoValue: {
    color: colors.text,
    fontSize: 14,
    fontWeight: "600",
  },
  inputField: {
    borderColor: colors.border,
    borderRadius: 10,
    borderWidth: 1,
    color: colors.text,
    fontSize: 15,
    marginTop: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  inputLabel: {
    color: colors.text,
    fontSize: 14,
    fontWeight: "600",
    marginTop: 12,
  },
  instructionText: {
    color: colors.gray,
    fontSize: 14,
    lineHeight: 20,
  },
  loadingText: {
    color: colors.gray,
    fontSize: 14,
    marginTop: 8,
  },
  locationIcon: {
    marginRight: 8,
    marginTop: 1,
  },
  previewImage: {
    borderRadius: 12,
    height: 180,
    width: "100%",
  },
  retryButton: {
    backgroundColor: colors.primary,
    borderRadius: 8,
    marginTop: 16,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  retryText: {
    color: colors.white,
    fontWeight: "600",
  },
  safeArea: {
    backgroundColor: colors.background,
    flex: 1,
  },
  scrollContainer: {
    padding: 16,
  },
  sectionCard: {
    backgroundColor: colors.white,
    borderColor: colors.border,
    borderRadius: 16,
    borderWidth: 1,
    marginTop: 14,
    padding: 20,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 16,
    fontWeight: "700",
    marginBottom: 4,
  },
  spinner: {
    marginTop: 16,
  },
  textArea: {
    height: 70,
    textAlignVertical: "top",
  },
  podDetailCard: {
    backgroundColor: "#F9FAFB",
    borderColor: colors.border,
    borderRadius: 8,
    borderWidth: 1,
    marginTop: 12,
    padding: 12,
  },
  podTitle: {
    color: colors.success,
    fontSize: 14,
    fontWeight: "700",
  },
  podNotes: {
    color: colors.text,
    fontSize: 13,
    marginTop: 4,
  },
});
