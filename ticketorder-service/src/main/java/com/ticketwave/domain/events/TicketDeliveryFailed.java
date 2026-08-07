package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.UUID;

public record TicketDeliveryFailed(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        String reason) implements DomainEvent {
}
