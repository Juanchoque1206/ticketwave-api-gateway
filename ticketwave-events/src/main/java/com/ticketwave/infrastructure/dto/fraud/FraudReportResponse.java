package com.ticketwave.infrastructure.dto.fraud;

import java.util.UUID;

public record FraudReportResponse(
        String userId,
        String ipAddress,
        String riskLevel,
        String signal,
        boolean blocked,
        boolean duplicate,
        String message
) {
}