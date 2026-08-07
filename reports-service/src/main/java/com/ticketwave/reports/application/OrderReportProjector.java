package com.ticketwave.reports.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.PaymentFailed;
import com.ticketwave.domain.events.TicketIssued;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderCompleted;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.events.TicketRefunded;
import org.springframework.stereotype.Component;

/**
 * Subscribes the reporting read model to the shared event bus. Thin by design:
 * it only registers the transactional projection service, mirroring the
 * event-driven subscriber pattern used across the platform.
 */
@Component
public class OrderReportProjector {

    private final OrderReportProjectionService projectionService;

    public OrderReportProjector(OrderReportProjectionService projectionService, EventBus eventBus) {
        this.projectionService = projectionService;
        eventBus.subscribe(TicketOrderCreated.class, projectionService::onCreated);
        eventBus.subscribe(TicketOrderConfirmed.class, projectionService::onConfirmed);
        eventBus.subscribe(TicketOrderCompleted.class, projectionService::onCompleted);
        eventBus.subscribe(TicketOrderCancelled.class, projectionService::onCancelled);
        eventBus.subscribe(PaymentAuthorized.class, projectionService::onPaymentAuthorized);
        eventBus.subscribe(PaymentFailed.class, projectionService::onPaymentFailed);
        eventBus.subscribe(TicketIssued.class, projectionService::onTicketIssued);
        eventBus.subscribe(TicketRefunded.class, projectionService::onTicketRefunded);
    }
}
