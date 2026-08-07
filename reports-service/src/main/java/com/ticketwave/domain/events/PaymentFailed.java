package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailed(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        BigDecimal total,
        String reason) implements DomainEvent {
}
