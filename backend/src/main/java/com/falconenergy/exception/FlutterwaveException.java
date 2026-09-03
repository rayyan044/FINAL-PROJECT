package com.falconenergy.exception;

/** Safe, structured Flutterwave failure. Never carries a raw provider response or credentials. */
public class FlutterwaveException extends RuntimeException {
    private final String stage;
    private final Integer httpStatus;
    private final String providerCode;
    private final String providerMessage;
    private final String traceId;

    public FlutterwaveException(String stage, Integer httpStatus, String providerCode, String providerMessage, String traceId, Throwable cause) {
        super(safeMessage(stage, httpStatus, providerCode, providerMessage), cause);
        this.stage = stage;
        this.httpStatus = httpStatus;
        this.providerCode = clean(providerCode);
        this.providerMessage = clean(providerMessage);
        this.traceId = clean(traceId);
    }
    public String getStage() { return stage; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getProviderCode() { return providerCode; }
    public String getProviderMessage() { return providerMessage; }
    public String getTraceId() { return traceId; }
    public static String safeMessage(String stage, Integer status, String code, String message) {
        String prefix = "Flutterwave " + cleanStage(stage) + " failed" + (status == null ? ". " : " (" + status + "): ");
        String detail = clean(message);
        return prefix + (detail == null ? (clean(code) == null ? "The provider did not return a safe error message." : "Provider error " + clean(code) + ".") : detail);
    }
    private static String cleanStage(String value) { return value == null || value.isBlank() ? "request" : value.replaceAll("[^A-Za-z -]", ""); }
    private static String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String safe = value.replaceAll("[\\r\\n\\t]", " ").replaceAll("(?i)(bearer|token|secret)\\s*[:=]\\s*[^ ,;]+", "$1=[redacted]").trim();
        return safe.length() > 300 ? safe.substring(0, 300) + "…" : safe;
    }
}
