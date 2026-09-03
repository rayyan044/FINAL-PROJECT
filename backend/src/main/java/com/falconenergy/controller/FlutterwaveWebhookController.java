package com.falconenergy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.falconenergy.config.FlutterwaveProperties;
import com.falconenergy.service.PaymentService;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@RestController @RequestMapping({"/api/v1/integrations/flutterwave", "/api/integrations/flutterwave"}) @RequiredArgsConstructor
public class FlutterwaveWebhookController {
    private final FlutterwaveProperties properties; private final PaymentService payments; private final ObjectMapper mapper;
    @PostMapping(value="/webhook", consumes="application/json")
    public ResponseEntity<Void> webhook(@RequestBody byte[] body, HttpServletRequest request) throws Exception {
        String signature=request.getHeader("flutterwave-signature");
        if(!valid(body, signature)) { log.warn("Rejected Flutterwave webhook: invalid or missing signature"); return ResponseEntity.status(401).build(); }
        JsonNode event;
        try { event=mapper.readTree(body); } catch (Exception ignored) { log.warn("Rejected Flutterwave webhook: invalid JSON"); return ResponseEntity.badRequest().build(); }
        JsonNode data=event.path("data");
        String eventId=event.path("id").asText(), chargeId=data.path("id").asText(), reference=data.path("reference").asText();
        if(eventId.isBlank()||chargeId.isBlank()||reference.isBlank()) { log.warn("Rejected Flutterwave webhook: missing event identifiers"); return ResponseEntity.badRequest().build(); }
        payments.processFlutterwaveWebhook(eventId, event.path("type").asText(), chargeId, reference);
        return ResponseEntity.ok().build();
    }
    /** Browser return only. It never trusts query parameters or changes payment state. */
    @GetMapping("/return")
    public ResponseEntity<Void> paymentReturn() {
        String target = properties.getFrontendReturnUrl();
        if (target == null || !target.startsWith("http")) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, target).build();
    }
    private boolean valid(byte[] body,String signature) throws Exception { return validSignature(body, signature, properties.getWebhookSecretHash()); }
    static boolean validSignature(byte[] body,String signature,String secret) throws Exception { if(secret==null||secret.isBlank()||signature==null||signature.isBlank()) return false; Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return MessageDigest.isEqual(Base64.getEncoder().encode(mac.doFinal(body)),signature.getBytes(StandardCharsets.UTF_8)); }
}
