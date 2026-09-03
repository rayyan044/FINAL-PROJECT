package com.falconenergy.controller;

import com.falconenergy.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class FlutterwaveWebhookSecurityTest {
    private static final class TestJwtFilter extends JwtAuthenticationFilter {
        TestJwtFilter() { super(null, null); }
        boolean skipped(MockHttpServletRequest request) { return shouldNotFilter(request); }
    }

    @Test void onlyExactWebhookPostsBypassJwtFilter() {
        TestJwtFilter filter = new TestJwtFilter();
        MockHttpServletRequest v1 = new MockHttpServletRequest("POST", "/api/v1/integrations/flutterwave/webhook");
        MockHttpServletRequest legacy = new MockHttpServletRequest("POST", "/api/integrations/flutterwave/webhook");
        MockHttpServletRequest protectedPayment = new MockHttpServletRequest("POST", "/api/v1/customer-portal/invoices/1/pay");
        MockHttpServletRequest webhookGet = new MockHttpServletRequest("GET", "/api/v1/integrations/flutterwave/webhook");
        assertTrue(filter.skipped(v1)); assertTrue(filter.skipped(legacy));
        assertFalse(filter.skipped(protectedPayment)); assertFalse(filter.skipped(webhookGet));
    }

    @Test void validatesOnlyTheV4RawBodyHmacSignature() throws Exception {
        byte[] body = "{\"id\":\"wbk_1\"}".getBytes(StandardCharsets.UTF_8); String secret = "sandbox-webhook-secret";
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(body));
        assertTrue(FlutterwaveWebhookController.validSignature(body, signature, secret));
        assertFalse(FlutterwaveWebhookController.validSignature(body, null, secret));
        assertFalse(FlutterwaveWebhookController.validSignature(body, signature, "wrong-secret"));
        assertFalse(FlutterwaveWebhookController.validSignature("{}".getBytes(StandardCharsets.UTF_8), signature, secret));
    }
}
