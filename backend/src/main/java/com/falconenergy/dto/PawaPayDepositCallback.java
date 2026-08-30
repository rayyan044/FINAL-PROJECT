package com.falconenergy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** The subset of pawaPay's v1/v2 deposit callback needed for reconciliation. */
public record PawaPayDepositCallback(
        @NotBlank String depositId,
        @NotBlank String status,
        @JsonAlias({"amount", "requestedAmount"}) BigDecimal amount,
        @NotBlank String currency,
        String providerTransactionId,
        FailureReason failureReason
) {
    public String failureMessage() {
        return failureReason == null ? null : failureReason.failureMessage();
    }

    public record FailureReason(String failureCode, String failureMessage) { }
}
