package com.ticketwave.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by the TicketOrder service once the payment for an order has been
 * authorized (consumes {@code PaymentAuthorized}), signalling that the order is
 * confirmed and ready for ticket issuance.
 */
public record TicketOrderConfirmed(
        UUID id,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        UUID eventId,
        BigDecimal total) implements DomainEvent {
}
