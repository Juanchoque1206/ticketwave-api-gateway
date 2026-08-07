package com.ticketwave.ticketorder.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read model of an Event as seen from the TicketOrder service. Only the fields
 * needed to price and reserve an order cross the service boundary.
 */
public record EventData(
        UUID id,
        String name,
        BigDecimal basePrice,
        int availableCount,
        String status) {
}
