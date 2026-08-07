package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionApplied(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        String promotionCode,
        BigDecimal discount) implements DomainEvent {
}
