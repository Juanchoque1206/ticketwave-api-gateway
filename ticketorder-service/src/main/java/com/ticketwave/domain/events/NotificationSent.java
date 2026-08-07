package com.ticketwave.domain.events;

import java.time.Instant;
import java.util.UUID;

public record NotificationSent(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        UUID notificationId) implements DomainEvent {
}
