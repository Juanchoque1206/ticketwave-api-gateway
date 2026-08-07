package com.ticketwave.reports.infrastructure.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketReportResponse(
        UUID id,
        String qrCode,
        UUID orderId,
        UUID userId,
        String username,
        UUID eventId,
        String eventName,
        BigDecimal price,
        String seat,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime validatedAt,
        LocalDateTime refundedAt) {
}
