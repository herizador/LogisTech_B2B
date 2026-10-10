package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String customerName,
    String address,
    Double lat,
    Double lng,
    BigDecimal weightKg,
    String status,
    Instant windowStart,
    Instant windowEnd,
    Instant createdAt) {
}
