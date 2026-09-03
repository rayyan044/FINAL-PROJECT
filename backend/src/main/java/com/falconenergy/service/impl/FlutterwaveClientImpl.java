package com.falconenergy.service.impl;

import com.falconenergy.config.FlutterwaveProperties;
import com.falconenergy.exception.FlutterwaveException;
import com.falconenergy.service.FlutterwaveClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlutterwaveClientImpl implements FlutterwaveClient {
    /** Flutterwave's V4 customer-name validation rules for an individual name field. */
    private static final Pattern VALID_CUSTOMER_NAME = Pattern.compile("[\\p{L}][\\p{L} ,.\\'-]*");
    private final FlutterwaveProperties properties;
    private volatile String token;
    private volatile Instant expiresAt = Instant.EPOCH;

    @Override
    public ChargeResult createMobileMoneyCharge(String email, String name, String phone, String network, BigDecimal amount, String currency, String reference) {
        Map<?, ?> customer;
        try {
            customer = post("customer creation", "/customers", Map.of("email", email, "name", Map.of("first", flutterwaveFirstName(name), "last", "Customer")), UUID.randomUUID().toString());
        } catch (FlutterwaveException exception) {
            // A customer can validly exist after a previous charge attempt. Reuse it rather
            // than converting a normal v4 conflict response into a failed payment.
            if (Integer.valueOf(409).equals(exception.getHttpStatus())) {
                customer = post("customer lookup", "/customers/search", Map.of("email", email), UUID.randomUUID().toString());
            } else {
                throw exception;
            }
        }
        String customerId = customerId(customer);
        if (customerId == null || customerId.isBlank()) throw failure("customer creation", null, null, "Flutterwave did not return a customer ID.", null, null);

        // Flutterwave V4 Tanzania mobile-money values: country dial code 255, Airtel network, local 9-digit MSISDN, and TZS charge currency.
        Map<?, ?> method = post("mobile-money payment-method creation", "/payment-methods", Map.of("type", "mobile_money", "mobile_money", Map.of("country_code", "255", "network", network, "phone_number", localPhone(phone))), UUID.randomUUID().toString());
        String methodId = string(data(method).get("id"));
        if (methodId == null || methodId.isBlank()) throw failure("mobile-money payment-method creation", null, null, "Flutterwave did not return a payment method ID.", null, null);

        Map<String, Object> charge = new LinkedHashMap<>();
        charge.put("amount", amount); // BigDecimal serializes as a JSON number, as required by V4.
        charge.put("currency", currency);
        charge.put("reference", reference);
        charge.put("customer_id", customerId);
        charge.put("payment_method_id", methodId);
        charge.put("meta", Map.of("source", "falcon-fuel", "reference", reference));
        if (properties.getRedirectUrl() != null && !properties.getRedirectUrl().isBlank()) charge.put("redirect_url", properties.getRedirectUrl());
        return result(post("charge creation", "/charges", charge, reference));
    }

    @Override public ChargeResult retrieveCharge(String chargeId) { return result(get("charge retrieval", "/charges/" + chargeId)); }

    private Map<?, ?> post(String stage, String uri, Object body, String idempotency) {
        try {
            return request().post().uri(uri).contentType(MediaType.APPLICATION_JSON).header("X-Trace-Id", trace()).header("X-Idempotency-Key", idempotency).headers(this::scenario).body(body).retrieve().body(Map.class);
        } catch (RestClientResponseException e) { throw fromResponse(stage, e); }
        catch (RestClientException e) { throw failure(stage, null, null, "Unable to reach Flutterwave.", null, e); }
    }
    private Map<?, ?> get(String stage, String uri) {
        try { return request().get().uri(uri).header("X-Trace-Id", trace()).headers(this::scenario).retrieve().body(Map.class); }
        catch (RestClientResponseException e) { throw fromResponse(stage, e); }
        catch (RestClientException e) { throw failure(stage, null, null, "Unable to reach Flutterwave.", null, e); }
    }
    private RestClient request() {
        if (!properties.enabled()) throw failure("configuration", null, null, "Flutterwave is not configured.", null, null);
        return RestClient.builder().baseUrl(properties.getBaseUrl().replaceAll("/$", "")).defaultHeader("Authorization", "Bearer " + accessToken()).build();
    }
    private void scenario(org.springframework.http.HttpHeaders headers) {
        if (!"sandbox".equalsIgnoreCase(properties.getEnvironment())) return;
        // Sandbox cannot send a real Tanzania wallet prompt. The redirect scenario makes
        // the tester explicitly choose success, failure, or cancellation instead of the
        // provider's default mocked mobile-money flow completing by itself.
        String key = properties.getScenarioKey();
        headers.set("X-Scenario-Key", key == null || key.isBlank() ? "scenario:auth_redirect" : key);
    }
    private String accessToken() {
        if (token != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) return token;
        synchronized (this) {
            if (token != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) return token;
            try {
                Map<?, ?> response = RestClient.create().post().uri(properties.getTokenUrl()).contentType(MediaType.APPLICATION_FORM_URLENCODED).body("client_id=" + enc(properties.getClientId()) + "&client_secret=" + enc(properties.getClientSecret()) + "&grant_type=client_credentials").retrieve().body(Map.class);
                token = string(response.get("access_token"));
                if (token == null || token.isBlank()) throw failure("OAuth token", null, null, "Flutterwave did not return an access token.", null, null);
                Object expiry = response.get("expires_in");
                expiresAt = Instant.now().plusSeconds(expiry == null ? 600 : Long.parseLong(String.valueOf(expiry)));
                return token;
            } catch (FlutterwaveException e) { throw e; }
            catch (RestClientResponseException e) { throw fromResponse("OAuth token", e); }
            catch (RestClientException e) { throw failure("OAuth token", null, null, "Unable to reach Flutterwave.", null, e); }
        }
    }
    private ChargeResult result(Map<?, ?> response) {
        Map<?, ?> d = data(response); Map<?, ?> next = map(d.get("next_action"));
        Map<?, ?> redirect = map(next.get("redirect_url")); Map<?, ?> instruction = map(next.get("payment_instruction"));
        String url = redirect.isEmpty() ? string(next.get("url")) : string(redirect.get("url"));
        String note = first(instruction.get("note"), instruction.get("message"), next.get("instruction"), next.get("message"));
        Map<?, ?> processor = map(d.get("processor_response"));
        return new ChargeResult(string(d.get("id")), string(d.get("reference")), first(d.get("provider_reference"), d.get("flw_ref"), processor.get("reference")),
                decimal(d.get("amount")), string(d.get("currency")), string(d.get("status")), string(next.get("type")), url, note, string(d.get("failure_reason")));
    }
    static FlutterwaveException fromResponse(String stage, RestClientResponseException exception) {
        Map<?, ?> response = parse(exception.getResponseBodyAsString()); Map<?, ?> error = map(response.get("error"));
        String code = first(error.get("code"), error.get("type"), response.get("code"));
        String message = first(validationMessage(error), error.get("message"), response.get("message"));
        String trace = exception.getResponseHeaders() == null ? null : first(exception.getResponseHeaders().getFirst("X-Trace-Id"), exception.getResponseHeaders().getFirst("X-Request-Id"), exception.getResponseHeaders().getFirst("Request-Id"));
        return failure(stage, exception.getStatusCode().value(), code, message, trace, exception);
    }
    private static Map<?, ?> parse(String body) { try { return map(new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class)); } catch (Exception ignored) { return Map.of(); } }
    @SuppressWarnings("unchecked") private static Map<?, ?> map(Object value) { return value instanceof Map<?, ?> map ? map : Map.of(); }
    private static Map<?, ?> data(Map<?, ?> response) { return map(response == null ? null : response.get("data")); }
    private static String customerId(Map<?, ?> response) {
        Map<?, ?> payload = data(response);
        String direct = string(payload.get("id"));
        if (direct != null && !direct.isBlank()) return direct;
        Object values = response == null ? null : response.get("data");
        if (values instanceof java.util.List<?> list && !list.isEmpty()) return string(map(list.getFirst()).get("id"));
        for (String key : java.util.List.of("customers", "content", "items")) {
            Object entries = payload.get(key);
            if (entries instanceof java.util.List<?> list && !list.isEmpty()) return string(map(list.getFirst()).get("id"));
        }
        return null;
    }
    private static String first(Object... values) { for (Object value : values) if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value); return null; }
    private static String validationMessage(Map<?, ?> error) { Object values = error.get("validation_errors"); if (!(values instanceof java.util.List<?> list) || list.isEmpty()) return null; Map<?, ?> first = map(list.getFirst()); String field = string(first.get("field_name")); String message = string(first.get("message")); return field == null || message == null ? null : field + ": " + message; }
    private static String string(Object value) { return value == null ? null : String.valueOf(value); }
    private static BigDecimal decimal(Object value) { return value == null ? null : new BigDecimal(String.valueOf(value)); }
    private static String trace() { return "falcon" + UUID.randomUUID().toString().replace("-", ""); }
    /**
     * The contact-person field is user-entered and historically has contained values such
     * as a one-letter initial or an ID number.  Never let that make the gateway request
     * invalid: Flutterwave requires a 2-50 character alphabetic name field.
     */
    static String flutterwaveFirstName(String value) {
        String candidate = value == null ? "" : value.trim().split("\\s+")[0];
        return candidate.length() >= 2 && candidate.length() <= 50 && VALID_CUSTOMER_NAME.matcher(candidate).matches()
                ? candidate
                : "Customer";
    }
    private static String localPhone(String phone) { String digits = phone.replaceAll("\\D", ""); return digits.startsWith("255") ? digits.substring(3) : digits; }
    private static String enc(String value) { return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8); }
    private static FlutterwaveException failure(String stage, Integer status, String code, String message, String trace, Throwable cause) {
        FlutterwaveException exception = new FlutterwaveException(stage, status, code, message, trace, cause);
        log.warn("Flutterwave failure stage={} status={} providerCode={} traceId={} message={}", exception.getStage(), exception.getHttpStatus(), exception.getProviderCode(), exception.getTraceId(), exception.getMessage());
        return exception;
    }
}
