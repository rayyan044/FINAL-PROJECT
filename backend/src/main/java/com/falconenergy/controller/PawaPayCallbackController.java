package com.falconenergy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.PawaPayDepositCallback;
import com.falconenergy.dto.PaymentResponse;
import com.falconenergy.service.PawaPayCallbackVerifier;
import com.falconenergy.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/integrations/pawapay", "/api/integrations/pawapay"})
@RequiredArgsConstructor
public class PawaPayCallbackController {
    private final PawaPayCallbackVerifier callbackVerifier;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(value = "/deposits/callback", consumes = "application/json")
    public ResponseEntity<ApiResponse<PaymentResponse>> depositCallback(@RequestBody byte[] body, HttpServletRequest request) throws Exception {
        callbackVerifier.verify(request, body);
        PawaPayDepositCallback callback = objectMapper.readValue(body, PawaPayDepositCallback.class);
        if (!validator.validate(callback).isEmpty()) throw new IllegalArgumentException("Invalid pawaPay callback.");
        return ResponseEntity.ok(ApiResponse.success("pawaPay deposit callback processed", paymentService.processPawaPayDepositCallback(callback)));
    }
}
