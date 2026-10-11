package com.logistech.fms.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverResponse(UUID id, String fullName, BigDecimal vehicleCapacityKg) {
}
