package com.logistech.fms.web.dto;

import java.time.Instant;
import java.util.UUID;

public record StopDto(
    int seq,
    UUID orderId,
    String customerName,
    String address,
    Double lat,
    Double lng,
    Instant eta,
    Instant deliveredAt) {
}
