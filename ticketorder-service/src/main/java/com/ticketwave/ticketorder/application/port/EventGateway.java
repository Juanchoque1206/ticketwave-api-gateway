package com.ticketwave.ticketorder.application.port;

import java.util.UUID;

/**
 * Outbound port to the Event aggregate owned by the monolith. Capacity
 * reservation and release are synchronous REST operations on the monolith.
 */
public interface EventGateway {

    EventData getEvent(UUID eventId);

    /**
     * Reserves capacity for an order. Returns the available count after the
     * reservation; throws if the event is cancelled or has insufficient capacity.
     */
    int reserveCapacity(UUID eventId, int quantity);

    void releaseCapacity(UUID eventId, int quantity);
}
