package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteVerifyDto(
    UUID routeId,
    String companyName,
    String date,
    String driverName,
    BigDecimal totalKm,
    int stops,
    int delivered) {
}
