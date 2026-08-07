package com.ticketwave.ticketorder.infrastructure.gateway;

import com.ticketwave.ticketorder.application.port.PromotionGateway;
import com.ticketwave.ticketorder.application.port.PromotionQuote;
import com.ticketwave.ticketorder.infrastructure.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test double for the PromotionGateway.
 */
public class FakePromotionGateway implements PromotionGateway {

    private final Map<String, BigDecimal> discounts = new ConcurrentHashMap<>();
    private final AtomicInteger usageIncrements = new AtomicInteger();

    public void seed(String code, BigDecimal discount) {
        discounts.put(code, discount);
    }

    public int usageIncrements() {
        return usageIncrements.get();
    }

    public void reset() {
        usageIncrements.set(0);
    }

    @Override
    public PromotionQuote quote(String code, UUID eventId, int quantity, BigDecimal subtotal) {
        BigDecimal discount = discounts.get(code);
        if (discount == null) {
            throw new BusinessRuleException("Promotion not found");
        }
        return new PromotionQuote(code, discount);
    }

    @Override
    public void incrementUsage(String code) {
        usageIncrements.incrementAndGet();
    }
}
