package com.ticketwave.infrastructure.dto.fraud;

import java.util.UUID;

public record FraudGuardRequest(
        UUID userId,
        String ipAddress
) {
}
