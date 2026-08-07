package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketOrderCompleted(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        UUID eventId,
        List<UUID> ticketIds,
        BigDecimal total) implements DomainEvent {
}
