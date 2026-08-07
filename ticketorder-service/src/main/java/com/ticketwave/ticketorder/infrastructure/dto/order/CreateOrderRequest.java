package com.ticketwave.ticketorder.infrastructure.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID eventId,
        @NotNull @Positive Integer quantity,
        String promotionCode
) {
}
