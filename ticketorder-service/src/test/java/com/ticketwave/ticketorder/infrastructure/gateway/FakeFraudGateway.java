package com.ticketwave.ticketorder.infrastructure.gateway;

import com.ticketwave.ticketorder.application.port.FraudGateway;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test double for the FraudGateway.
 */
public class FakeFraudGateway implements FraudGateway {

    private final AtomicInteger guards = new AtomicInteger();
    private final AtomicInteger markedOrders = new AtomicInteger();

    public int guardCount() {
        return guards.get();
    }

    public int markedOrderCount() {
        return markedOrders.get();
    }

    public void reset() {
        guards.set(0);
        markedOrders.set(0);
    }

    @Override
    public void guard(UUID userId, String ipAddress) {
        guards.incrementAndGet();
    }

    @Override
    public void markOrder(UUID orderId, UUID userId) {
        markedOrders.incrementAndGet();
    }
}
