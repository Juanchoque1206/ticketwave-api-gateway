package com.ticketwave.ticketorder.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound port to the Promotion aggregate owned by the monolith.
 */
public interface PromotionGateway {

    PromotionQuote quote(String code, UUID eventId, int quantity, BigDecimal subtotal);

    void incrementUsage(String code);
}
