package com.ticketwave.ticketorder.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.ticketorder.infrastructure.dto.order.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * User-facing order cancellation (REST). Cancels the pending order, releases the
 * reserved capacity and publishes TicketOrderCancelled so the monolith can void
 * any pending payment and the orchestrator can converge.
 */
@Service
public class CancelOrderUseCase {

    private final OrderService orderService;
    private final EventBus eventBus;

    public CancelOrderUseCase(OrderService orderService, EventBus eventBus) {
        this.orderService = orderService;
        this.eventBus = eventBus;
    }

    @Transactional
    public void cancel(UUID orderId) {
        OrderResponse order = orderService.cancel(orderId);
        eventBus.publish(new TicketOrderCancelled(UUID.randomUUID(), Instant.now(),
                order.id(), order.userId(), order.eventId(), order.quantity()));
    }
}
