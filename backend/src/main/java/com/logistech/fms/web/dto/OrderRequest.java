package com.logistech.fms.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderRequest(
    @NotBlank String customerName,
    @NotBlank String address,
    Double lat,
    Double lng,
    BigDecimal weightKg,
    Instant windowStart,
    Instant windowEnd) {
}
