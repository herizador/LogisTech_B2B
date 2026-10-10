package com.logistech.fms.web.dto;

import java.util.UUID;

public record SkippedOrderDto(UUID orderId, String customerName, String reason) {
}
