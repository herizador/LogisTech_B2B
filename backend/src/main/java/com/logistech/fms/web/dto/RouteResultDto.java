package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RouteResultDto(
    UUID routeId,
    String driverName,
    BigDecimal totalKm,
    long drivingMinutes,
    List<StopDto> stops,
    List<List<Double>> geometry) {
}
