package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RouteSummaryDto(
    UUID routeId,
    LocalDate date,
    String driverName,
    BigDecimal totalKm,
    long stops) {
}
