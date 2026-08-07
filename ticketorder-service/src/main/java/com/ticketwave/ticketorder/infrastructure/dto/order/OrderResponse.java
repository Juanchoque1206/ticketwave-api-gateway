package com.ticketwave.ticketorder.infrastructure.dto.order;

import com.ticketwave.ticketorder.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        UUID eventId,
        String eventName,
        OrderStatus status,
        int quantity,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        LocalDateTime reservedAt,
        LocalDateTime expiresAt
) {
}
