package com.falconenergy.service.impl;

import com.falconenergy.exception.FlutterwaveException;
import com.falconenergy.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FlutterwaveClientImplTest {
    @Test void usesGatewaySafeFallbackForInvalidCustomerFirstName() {
        assertEquals("Abubakar", FlutterwaveClientImpl.flutterwaveFirstName("Abubakar Mussa"));
        assertEquals("Customer", FlutterwaveClientImpl.flutterwaveFirstName("A Mussa"));
        assertEquals("Customer", FlutterwaveClientImpl.flutterwaveFirstName("12345"));
        assertEquals("Customer", FlutterwaveClientImpl.flutterwaveFirstName("   "));
    }

    @Test void normalizesTanzanianNumbersAndMapsProviderStates() {
        assertEquals("255682328642", PaymentServiceImpl.normalizeTanzanianPhone("0682328642"));
        assertEquals("255682328642", PaymentServiceImpl.normalizeTanzanianPhone("+255682328642"));
        assertEquals("255682328642", PaymentServiceImpl.normalizeTanzanianPhone("255682328642"));
        assertEquals(PaymentStatus.ACTION_REQUIRED, PaymentServiceImpl.mapStatus("pending", "payment_instruction"));
        assertEquals(PaymentStatus.SUCCESSFUL, PaymentServiceImpl.mapStatus("succeeded", null));
        assertEquals(PaymentStatus.CANCELLED, PaymentServiceImpl.mapStatus("cancelled", null));
        assertEquals(PaymentStatus.CANCELLED, PaymentServiceImpl.mapStatus("failed", null, "Customer cancelled the authorization"));
        assertEquals(PaymentStatus.EXPIRED, PaymentServiceImpl.mapStatus("expired", null));
        assertEquals(PaymentStatus.UNKNOWN, PaymentServiceImpl.mapStatus("new-provider-state", null));
    }

    @Test void capturesSafe400ValidationError() {
        HttpHeaders headers = new HttpHeaders(); headers.add("X-Request-Id", "request-400");
        FlutterwaveException error = FlutterwaveClientImpl.fromResponse("charge creation", HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", headers, "{\"error\":{\"code\":\"10400\",\"message\":\"Currency not supported for TZ Mobile Money.\"}}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals(400, error.getHttpStatus()); assertEquals("10400", error.getProviderCode()); assertEquals("request-400", error.getTraceId());
        assertEquals("Flutterwave charge creation failed (400): Currency not supported for TZ Mobile Money.", error.getMessage());
    }
    @Test void prefersSafeFieldLevelValidationDetail() {
        FlutterwaveException error = FlutterwaveClientImpl.fromResponse("charge creation", HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", new HttpHeaders(), "{\"error\":{\"code\":\"10400\",\"message\":\"Request is not valid\",\"validation_errors\":[{\"field_name\":\"reference\",\"message\":\"size must be between 6 and 42\"}]}}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals("Flutterwave charge creation failed (400): reference: size must be between 6 and 42", error.getMessage());
    }
    @Test void captures401WithoutLeakingAuthorizationHeader() {
        HttpHeaders headers = new HttpHeaders(); headers.add("Authorization", "Bearer should-never-appear");
        FlutterwaveException error = FlutterwaveClientImpl.fromResponse("OAuth token", HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", headers, "{\"error\":{\"code\":\"invalid_client\",\"message\":\"Client authentication failed\"}}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals(401, error.getHttpStatus()); assertEquals("invalid_client", error.getProviderCode());
        assertFalse(error.getMessage().contains("should-never-appear"));
    }
    @Test void captures500WithoutRawResponseLeakage() {
        String raw = "{\"error\":{\"code\":\"server_error\",\"message\":\"Please retry later\"},\"access_token\":\"secret-token\"}";
        FlutterwaveException error = FlutterwaveClientImpl.fromResponse("customer creation", HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", new HttpHeaders(), raw.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals(500, error.getHttpStatus()); assertEquals("server_error", error.getProviderCode());
        assertEquals("Flutterwave customer creation failed (500): Please retry later", error.getMessage());
        assertFalse(error.getMessage().contains("secret-token"));
    }
}
