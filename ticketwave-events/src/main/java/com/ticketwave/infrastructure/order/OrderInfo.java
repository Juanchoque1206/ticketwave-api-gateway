package com.ticketwave.infrastructure.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model of an order as exposed by the ticketorder-service. Only the fields
 * needed to charge and compensate an order cross the service boundary.
 */
public record OrderInfo(
        UUID id,
        UUID userId,
        UUID eventId,
        String eventName,
        String status,
        int quantity,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        LocalDateTime reservedAt,
        LocalDateTime expiresAt) {
}
