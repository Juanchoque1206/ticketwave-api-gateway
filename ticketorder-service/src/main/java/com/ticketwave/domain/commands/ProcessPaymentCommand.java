package com.ticketwave.domain.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProcessPaymentCommand(
        UUID commandId,
        Instant issuedAt,
        UUID orderId,
        String provider,
        BigDecimal amount) implements Command {
}
