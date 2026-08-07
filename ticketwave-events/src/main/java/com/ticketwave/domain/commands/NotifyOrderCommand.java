package com.ticketwave.domain.commands;

import java.time.Instant;
import java.util.UUID;

public record NotifyOrderCommand(
        UUID commandId,
        Instant issuedAt,
        UUID orderId,
        UUID userId,
        UUID eventId) implements Command {
}
