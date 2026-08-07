package com.ticketwave.infrastructure.dto.fraud;

import java.util.UUID;

public record MarkOrderRequest(
        UUID orderId,
        UUID userId
) {
}
