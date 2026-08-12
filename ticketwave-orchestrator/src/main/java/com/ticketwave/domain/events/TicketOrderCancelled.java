package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.UUID;

public record TicketOrderCancelled(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        UUID eventId,
        int quantity) implements DomainEvent {
}