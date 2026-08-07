package com.ticketwave.ticketorder.infrastructure.rest;

import java.math.BigDecimal;

public record PromotionQuoteResponse(
        String code,
        BigDecimal discount) {
}
