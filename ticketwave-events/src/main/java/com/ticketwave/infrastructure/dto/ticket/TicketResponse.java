package com.ticketwave.infrastructure.dto.ticket;

import com.ticketwave.domain.ticket.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String qrCode,
        UUID orderId,
        UUID eventId,
        String eventName,
        BigDecimal price,
        String seat,
        TicketStatus status,
        LocalDateTime issuedAt
) {
}