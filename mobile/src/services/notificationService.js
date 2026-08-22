import api from "../api/api";

function getErrorMessage(error) {
  if (!error.response) {
    return "Unable to reach the service. Check your internet connection and try again.";
  }

  if (error.response.status === 401 || error.response.status === 403) {
    return "Your session has expired. Please sign in again.";
  }

  const message = error.response.data?.message || error.response.data?.error;
  return typeof message === "string" && message.trim()
    ? message
    : "We could not update notifications. Please try again.";
}

export async function getNotifications() {
  try {
    const response = await api.get("/mobile/notifications");
    return response.data?.data || [];
  } catch (error) {
    throw new Error(getErrorMessage(error));
  }
}

export async function markAsRead(notificationId) {
  try {
    const response = await api.patch(`/mobile/notifications/${notificationId}/read`);
    return response.data;
  } catch (error) {
    throw new Error(getErrorMessage(error));
  }
}
