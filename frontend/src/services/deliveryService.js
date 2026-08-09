import { api } from "./api";

export async function getActiveDeliveries() {
  return api.get("/deliveries/active").then((r) => r.data);
}

export async function createDeliveryRecord(dispatchId) {
  return api.post(`/deliveries/create/${dispatchId}`).then((r) => r.data);
}

export async function getDeliveryById(id) {
  return api.get(`/deliveries/${id}`).then((r) => r.data);
}

export async function recordArrival(id, payload) {
  return api.post(`/deliveries/${id}/arrival`, payload).then((r) => r.data);
}

export async function completeDelivery(id, payload) {
  return api.post(`/deliveries/${id}/complete`, payload).then((r) => r.data);
}

export async function cancelDelivery(id, remarks) {
  return api.post(`/deliveries/${id}/cancel`, null, { params: { remarks } }).then((r) => r.data);
}

export async function getDeliveryHistory() {
  return api.get("/deliveries/history").then((r) => r.data);
}

export async function listDeliveries() {
  const storedUser = sessionStorage.getItem("feftms_user");
  const parsedUser = storedUser ? JSON.parse(storedUser) : null;
  const isDriver = parsedUser?.role === "DRIVER";

  if (isDriver) {
    // Driver workspace data is scoped by the JWT on the mobile endpoint.
    return api.get("/mobile/deliveries").then((r) => ({
      content: (r.data || []).map((delivery) => ({
        ...delivery,
        id: delivery.deliveryId,
        deliveryStatus: delivery.currentStatus,
        order: {
          customerName: delivery.customerName,
          productName: delivery.fuelProduct,
          quantity: delivery.quantity,
        },
      })),
    }));
  } else {
    // For operations, dispatcher, manager, admin, etc.
    return api.get("/deliveries/active").then((r) => ({
      content: r.data || [],
      totalElements: (r.data || []).length,
    }));
  }
}
export async function updateDeliveryStatus(id, status) {
  if (status === "ARRIVED_AT_DESTINATION") {
    return recordArrival(id, { receivedBy: "operations", remarks: "Updated trip status" });
  } else if (status === "DELIVERED") {
    return completeDelivery(id, { completedBy: "operations", remarks: "Updated trip status" });
  } else if (status === "CANCELLED") {
    return cancelDelivery(id, "Updated trip status");
  }
  throw new Error(`Unsupported delivery status transition: ${status}`);
}

export const createDelivery = createDeliveryRecord;
