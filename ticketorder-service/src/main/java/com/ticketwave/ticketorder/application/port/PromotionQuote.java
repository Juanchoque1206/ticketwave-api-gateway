package com.ticketwave.ticketorder.application.port;

import java.math.BigDecimal;

/**
 * Result of quoting a promotion code against a pending order.
 */
public record PromotionQuote(
        String code,
        BigDecimal discount) {
}
