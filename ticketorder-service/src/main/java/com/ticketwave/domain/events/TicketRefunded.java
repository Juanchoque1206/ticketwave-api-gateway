package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketRefunded(
        UUID id,
        Instant occurredAt,
        UUID ticketId,
        UUID orderId,
        UUID userId,
        BigDecimal amount) implements DomainEvent {
}
