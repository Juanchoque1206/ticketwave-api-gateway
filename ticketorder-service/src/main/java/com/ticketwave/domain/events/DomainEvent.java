package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for every domain event published inside the application.
 * Implementations are immutable records carrying only the data relevant to the
 * event so that producers and consumers stay loosely coupled.
 */
public interface DomainEvent {

    UUID id();

    Instant occurredAt();
}
