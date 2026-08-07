package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketOrderCreated(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        UUID eventId,
        int quantity,
        BigDecimal total,
        BigDecimal discount) implements DomainEvent {
}
