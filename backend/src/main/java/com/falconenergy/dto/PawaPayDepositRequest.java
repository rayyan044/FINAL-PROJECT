package com.falconenergy.dto;

import jakarta.validation.constraints.NotBlank;

public record PawaPayDepositRequest(@NotBlank String paymentMethod, @NotBlank String phoneNumber) { }
