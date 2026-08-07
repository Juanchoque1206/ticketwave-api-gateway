package com.ticketwave.domain.commands;

import java.time.Instant;
import java.util.UUID;

public record CancelTicketOrderCommand(
        UUID commandId,
        Instant issuedAt,
        UUID orderId,
        UUID userId,
        UUID eventId,
        int quantity,
        String reason) implements Command {
}
