package com.falconenergy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.falconenergy.config.FlutterwaveProperties;
import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.PaymentResponse;
import com.falconenergy.service.PaymentService;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@RestController @RequestMapping({"/api/v1/integrations/flutterwave", "/api/integrations/flutterwave"}) @RequiredArgsConstructor
public class FlutterwaveWebhookController {
    private final FlutterwaveProperties properties; private final PaymentService payments; private final ObjectMapper mapper;
    @PostMapping(value="/webhook", consumes="application/json")
    public ResponseEntity<ApiResponse<PaymentResponse>> webhook(@RequestBody byte[] body, HttpServletRequest request) throws Exception {
        String signature=request.getHeader("flutterwave-signature");
        if(!valid(body, signature)) return ResponseEntity.status(401).build();
        JsonNode data=mapper.readTree(body).path("data");
        PaymentResponse result=payments.processFlutterwaveWebhook(data.path("id").asText(), data.path("reference").asText(), data.path("status").asText(), data.hasNonNull("amount")?data.decimalValue():null, data.path("currency").asText());
        return ResponseEntity.ok(ApiResponse.success("Flutterwave webhook processed", result));
    }
    private boolean valid(byte[] body,String signature) throws Exception { if(properties.getWebhookSecretHash()==null||properties.getWebhookSecretHash().isBlank()||signature==null) return false; Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(properties.getWebhookSecretHash().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return MessageDigest.isEqual(Base64.getEncoder().encode(mac.doFinal(body)),signature.getBytes(StandardCharsets.UTF_8)); }
}
