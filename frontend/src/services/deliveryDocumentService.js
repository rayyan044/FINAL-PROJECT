import { api } from "./api";

export async function getDeliveryNote(activityId) {
  return api.get(`/delivery-notes/loading-activity/${activityId}`).then((r) => r.data);
}

export async function getPaymentReceipt(invoiceId) {
  return api.get(`/payment-receipts/invoice/${invoiceId}`).then((r) => r.data);
}

export async function getPaymentReceiptForOrder(orderId) {
  return api.get(`/payment-receipts/order/${orderId}`).then((r) => r.data);
}

export async function printDeliveryNote(noteId) {
  return api.post(`/delivery-notes/${noteId}/print`).then((r) => r.data);
}

export async function handDeliveryNoteToDriver(noteId) {
  return api.post(`/delivery-notes/${noteId}/handover`).then((r) => r.data);
}
