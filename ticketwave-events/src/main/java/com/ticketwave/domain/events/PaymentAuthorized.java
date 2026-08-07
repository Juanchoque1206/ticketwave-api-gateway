package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAuthorized(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        BigDecimal total,
        String providerTransactionId) implements DomainEvent {
}