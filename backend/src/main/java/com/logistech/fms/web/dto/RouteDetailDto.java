package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RouteDetailDto(
    UUID routeId,
    String date,
    String driverName,
    BigDecimal totalKm,
    List<StopDto> stops) {
}
