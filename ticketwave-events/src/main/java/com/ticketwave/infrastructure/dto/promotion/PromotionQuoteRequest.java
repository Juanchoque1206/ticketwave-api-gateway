package com.ticketwave.infrastructure.dto.promotion;

import java.math.BigDecimal;
import java.util.UUID;

public record PromotionQuoteRequest(
        UUID eventId,
        int quantity,
        BigDecimal subtotal
) {
}
