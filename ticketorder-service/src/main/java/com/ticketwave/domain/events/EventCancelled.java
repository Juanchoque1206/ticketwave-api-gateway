package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.UUID;

public record EventCancelled(
        UUID id,
        Instant occurredAt,
        UUID eventId) implements DomainEvent {
}
