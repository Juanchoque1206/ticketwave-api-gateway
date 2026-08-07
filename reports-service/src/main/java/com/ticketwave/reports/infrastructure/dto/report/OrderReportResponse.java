package com.ticketwave.reports.infrastructure.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderReportResponse(
        UUID orderId,
        UUID userId,
        UUID eventId,
        String eventName,
        int quantity,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String status,
        LocalDateTime reservedAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt,
        String paymentStatus,
        String providerTransactionId,
        int ticketCount,
        BigDecimal refundedAmount) {
}
