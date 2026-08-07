package com.ticketwave.infrastructure.dto.ticket;

import com.ticketwave.domain.ticket.TicketStatus;

public record ValidateTicketResponse(
        String qrCode,
        String eventName,
        String seat,
        TicketStatus status,
        boolean valid,
        String message
) {
}