import api from "../api/api";

function getErrorMessage(error) {
  if (!error.response) {
    return "Unable to reach the service. Check your internet connection and try again.";
  }

  if (error.response.status === 401 || error.response.status === 403) {
    return "Your session has expired. Please sign in again.";
  }

  if (error.response.status >= 500) {
    return "The service is temporarily unavailable. Please try again shortly.";
  }

  const message = error.response.data?.message || error.response.data?.error;
  return typeof message === "string" && message.trim()
    ? message
    : "We could not load your deliveries. Please try again.";
}

function valueFrom(delivery, ...keys) {
  return keys.map((key) => delivery?.[key]).find((value) => value !== undefined && value !== null);
}

function normalizeDelivery(delivery) {
  return {
    ...delivery,
    assignedAt: valueFrom(delivery, "assignedAt", "assignedDate", "createdAt"),
    customerName: valueFrom(delivery, "customerName", "customer", "customer_name"),
    deliveryId: valueFrom(delivery, "deliveryId", "id"),
    deliveryNoteNumber: valueFrom(delivery, "deliveryNoteNumber", "deliveryNoteNo", "delivery_note_number"),
    destination: valueFrom(delivery, "destination", "deliveryAddress", "delivery_address"),
    fuelProduct: valueFrom(delivery, "fuelProduct", "productName", "product", "fuel_product"),
    quantity: valueFrom(delivery, "quantity", "quantityLitres", "quantity_litres"),
    scheduledDeliveryDate: valueFrom(delivery, "scheduledDeliveryDate", "scheduledDate", "scheduled_delivery_date"),
    currentStatus: valueFrom(delivery, "currentStatus", "status", "deliveryStatus", "delivery_status"),
  };
}

export async function getMyDeliveries() {
  try {
    // The authenticated Axios interceptor attaches the JWT. The driver identity
    // is intentionally determined by the server; no driver ID is sent here.
    const response = await api.get("/mobile/deliveries");
    const envelope = response.data;
    const deliveries = Array.isArray(envelope?.data)
      ? envelope.data
      : Array.isArray(envelope?.data?.deliveries)
        ? envelope.data.deliveries
      : Array.isArray(envelope)
        ? envelope
        : null;

    if (!deliveries) {
      throw new Error("The deliveries service returned an unexpected response.");
    }

    return deliveries.map(normalizeDelivery);
  } catch (error) {
    if (error.message === "The deliveries service returned an unexpected response.") {
      throw error;
    }

    throw new Error(getErrorMessage(error));
  }
}

export async function getMyDelivery(deliveryId) {
  try {
    const response = await api.get(`/mobile/deliveries/${deliveryId}`);
    if (!response.data?.data || typeof response.data.data !== "object") {
      throw new Error("The delivery service returned an unexpected response.");
    }
    return normalizeDelivery(response.data.data);
  } catch (error) {
    if (error.message === "The delivery service returned an unexpected response.") throw error;
    throw new Error(getErrorMessage(error));
  }
}
