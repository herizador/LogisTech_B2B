package com.logistech.fms.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record DriverRequest(
    @NotBlank String fullName,
    BigDecimal vehicleCapacityKg) {
}
