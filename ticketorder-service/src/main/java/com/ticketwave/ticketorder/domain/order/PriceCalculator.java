package com.ticketwave.ticketorder.domain.order;

import java.math.BigDecimal;

public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static BigDecimal totalFor(TicketOrder order, BigDecimal basePrice, BigDecimal discountAmount) {
        BigDecimal quantity = BigDecimal.valueOf(order.getQuantity());
        return basePrice.multiply(quantity).subtract(discountAmount);
    }

    public static boolean isExpired(TicketOrder order) {
        return order.getExpiresAt() != null && order.getExpiresAt().isBefore(java.time.LocalDateTime.now());
    }

    public static boolean isActive(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }
}
