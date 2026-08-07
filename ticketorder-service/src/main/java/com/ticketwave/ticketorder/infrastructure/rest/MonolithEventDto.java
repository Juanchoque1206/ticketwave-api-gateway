package com.ticketwave.ticketorder.infrastructure.rest;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Subset of the monolith EventResponse consumed by the order service.
 */
public record MonolithEventDto(
        UUID id,
        String name,
        BigDecimal basePrice,
        int availableCount,
        String status) {
}
