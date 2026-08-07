package com.ticketwave.reports.infrastructure.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentReportResponse(
        UUID id,
        UUID orderId,
        String provider,
        String status,
        BigDecimal amount,
        String providerTransactionId,
        LocalDateTime paidAt) {
}
