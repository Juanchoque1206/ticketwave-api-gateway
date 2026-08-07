package com.ticketwave.infrastructure.payment;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.domain.events.TicketRefunded;
import com.ticketwave.domain.payment.Payment;
import com.ticketwave.domain.payment.PaymentRepository;
import com.ticketwave.domain.payment.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscriber adapter that mirrors order lifecycle domain events onto the
 * payment provider integration: captures, voids and refunds are driven by the
 * event bus instead of direct service calls.
 */
@Component
public class PaymentEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventSubscriber.class);

    private final PaymentRepository paymentRepository;

    public PaymentEventSubscriber(EventBus eventBus, PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
        eventBus.subscribe(PaymentAuthorized.class, this::onAuthorized);
        eventBus.subscribe(TicketOrderCancelled.class, this::onCancelled);
        eventBus.subscribe(TicketRefunded.class, this::onRefunded);
        eventBus.subscribe(TicketOrderConfirmed.class, this::onConfirmed);
    }

    private void onAuthorized(PaymentAuthorized event) {
        log.info("Payment provider: capture hold for order {} total {}", event.orderId(), event.total());
    }

    private void onConfirmed(TicketOrderConfirmed event) {
        log.info("Order {} confirmed by the ticketorder-service", event.orderId());
    }

    private void onCancelled(TicketOrderCancelled event) {
        paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });
        log.info("Payment provider: void hold for order {}", event.orderId());
    }

    private void onRefunded(TicketRefunded event) {
        paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        });
        log.info("Payment provider: refund {} for order {}", event.amount(), event.orderId());
    }
}