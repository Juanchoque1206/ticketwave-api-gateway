package com.ticketwave.domain.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundPaymentCommand(
        UUID commandId,
        Instant issuedAt,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String reason) implements Command {
}