package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketIssued(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        UUID eventId,
        List<UUID> ticketIds) implements DomainEvent {
}
