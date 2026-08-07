package com.ticketwave.ticketorder.infrastructure.rest;

import java.util.UUID;

public record MarkOrderRequest(
        UUID orderId,
        UUID userId) {
}
