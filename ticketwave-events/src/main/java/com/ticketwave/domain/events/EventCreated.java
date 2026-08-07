package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.UUID;

public record EventCreated(
        UUID id,
        Instant occurredAt,
        UUID eventId,
        String name,
        String city) implements DomainEvent {
}