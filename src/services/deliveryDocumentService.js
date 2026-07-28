import { api } from "./api";

export async function generateDeliveryNote(activityId) {
  return api.post(`/delivery-notes/loading-activity/${activityId}`).then((r) => r.data);
}

export async function generateTruckInvoice(activityId) {
  return api.post(`/truck-invoices/loading-activity/${activityId}`).then((r) => r.data);
}

export async function getDeliveryNote(activityId) {
  return api.get(`/delivery-notes/loading-activity/${activityId}`).then((r) => r.data);
}

export async function getTruckInvoice(activityId) {
  return api.get(`/truck-invoices/loading-activity/${activityId}`).then((r) => r.data);
}

export async function printDeliveryNote(noteId) {
  return api.post(`/delivery-notes/${noteId}/print`).then((r) => r.data);
}

export async function printTruckInvoice(invoiceId) {
  return api.post(`/truck-invoices/${invoiceId}/print`).then((r) => r.data);
}

export async function markHandedToDriver(noteId) {
  return api.post(`/delivery-notes/${noteId}/handover`).then((r) => r.data);
}
