package com.ticketwave.reports.infrastructure.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationReportResponse(
        UUID id,
        UUID userId,
        String username,
        String type,
        String channel,
        String subject,
        String body,
        boolean read,
        LocalDateTime createdAt) {
}
