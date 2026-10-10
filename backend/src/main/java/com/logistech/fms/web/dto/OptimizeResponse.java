package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OptimizeResponse(
    String date,
    double totalKm,
    long totalDrivingMinutes,
    List<RouteResultDto> routes,
    List<SkippedOrderDto> skipped) {
}

