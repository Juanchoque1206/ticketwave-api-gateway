package com.ticketwave.infrastructure.dto.promotion;

import java.math.BigDecimal;

public record PromotionQuoteResponse(
        String code,
        BigDecimal discount
) {
}
