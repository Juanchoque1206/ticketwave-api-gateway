package com.ticketwave.ticketorder.application.port;

import java.util.UUID;

/**
 * Outbound port to the Fraud aggregate owned by the monolith. The rate-limit
 * counter and duplicate-order signals stay in the monolith's Redis store.
 */
public interface FraudGateway {

    void guard(UUID userId, String ipAddress);

    void markOrder(UUID orderId, UUID userId);
}
