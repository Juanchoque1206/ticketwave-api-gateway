package com.ticketwave.ticketorder.infrastructure.rest;

import java.util.UUID;

public record FraudGuardRequest(
        UUID userId,
        String ipAddress) {
}
