package com.ticketwave.ticketorder.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.ticketorder.infrastructure.dto.order.OrderResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Event-driven order confirmation: consumes PaymentAuthorized (published by the
 * monolith payment service) and advances the order to CONFIRMED, emitting
 * TicketOrderConfirmed so the rest of the platform can react.
 */
@Component
public class ConfirmOrderOnPayment {

    private final OrderService orderService;
    private final EventBus eventBus;

    public ConfirmOrderOnPayment(EventBus eventBus, OrderService orderService) {
        this.orderService = orderService;
        this.eventBus = eventBus;
        eventBus.subscribe(PaymentAuthorized.class, this::onPaymentAuthorized);
    }

    private void onPaymentAuthorized(PaymentAuthorized event) {
        OrderResponse order = orderService.confirm(event.orderId());
        eventBus.publish(new TicketOrderConfirmed(UUID.randomUUID(), Instant.now(),
                order.id(), order.userId(), order.eventId(), order.totalAmount()));
    }
}
