package com.ticketwave.ticketorder.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.ticketorder.infrastructure.dto.order.CreateOrderRequest;
import com.ticketwave.ticketorder.infrastructure.dto.order.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Order creation entry point. Persists the pending order and publishes
 * TicketOrderCreated so the saga orchestrator (in the monolith) can start the
 * purchase workflow.
 */
@Service
public class CreateOrderUseCase {

    private final OrderService orderService;
    private final EventBus eventBus;

    public CreateOrderUseCase(OrderService orderService, EventBus eventBus) {
        this.orderService = orderService;
        this.eventBus = eventBus;
    }

    @Transactional
    public OrderResponse reserve(AuthenticationContext ctx, CreateOrderRequest request) {
        OrderResponse response = orderService.createReservation(ctx, request);
        eventBus.publish(new TicketOrderCreated(
                UUID.randomUUID(),
                Instant.now(),
                response.id(),
                response.userId(),
                response.eventId(),
                response.quantity(),
                response.totalAmount(),
                response.discountAmount()));
        return response;
    }
}
