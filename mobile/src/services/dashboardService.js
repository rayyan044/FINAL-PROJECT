import api from "../api/api";

function getMessage(error) {
  if (!error.response) {
    return "Unable to reach the service. Check your internet connection and try again.";
  }

  if (error.response.status >= 500) {
    return "The service is temporarily unavailable. Please try again shortly.";
  }

  const message = error.response.data?.message;
  return typeof message === "string" && message.trim()
    ? message
    : "We could not load your dashboard. Please try again.";
}

export async function getDashboard() {
  try {
    const response = await api.get("/mobile/dashboard");
    const envelope = response.data;

    if (
      !envelope ||
      envelope.success === false ||
      !envelope.data ||
      typeof envelope.data !== "object" ||
      !envelope.data.summary ||
      !Array.isArray(envelope.data.recentDeliveries)
    ) {
      throw new Error("The dashboard service returned an unexpected response.");
    }

    return envelope.data;
  } catch (error) {
    if (error.message === "The dashboard service returned an unexpected response.") {
      throw error;
    }

    throw new Error(getMessage(error));
  }
}
