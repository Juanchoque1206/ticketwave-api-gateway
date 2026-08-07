package com.ticketwave.reports.application;

import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.PaymentFailed;
import com.ticketwave.domain.events.TicketIssued;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderCompleted;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.events.TicketRefunded;
import com.ticketwave.reports.domain.order.OrderReport;
import com.ticketwave.reports.domain.order.OrderReportRepository;
import com.ticketwave.reports.domain.projection.EventProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Transactional write path for the {@code order_reports} read model. Each
 * handler upserts the row for the order it belongs to, so re-delivered messages
 * are idempotent for the state transitions (creation, status changes, payment
 * information, ticket count). Writes only ever touch the reporting service's own
 * table.
 */
@Service
public class OrderReportProjectionService {

    private final OrderReportRepository orderReportRepository;
    private final EventProjectionRepository eventProjectionRepository;

    public OrderReportProjectionService(OrderReportRepository orderReportRepository,
                                        EventProjectionRepository eventProjectionRepository) {
        this.orderReportRepository = orderReportRepository;
        this.eventProjectionRepository = eventProjectionRepository;
    }

    @Transactional
    public void onCreated(TicketOrderCreated event) {
        OrderReport report = upsert(event.orderId());
        report.setUserId(event.userId());
        report.setEventId(event.eventId());
        report.setEventName(eventProjectionRepository.findById(event.eventId())
                .map(e -> e.getName())
                .orElse(null));
        report.setQuantity(event.quantity());
        report.setTotalAmount(event.total());
        report.setDiscountAmount(event.discount());
        report.setStatus("PENDING");
        report.setReservedAt(toLocal(event.occurredAt()));
        touch(report);
    }

    @Transactional
    public void onConfirmed(TicketOrderConfirmed event) {
        OrderReport report = upsert(event.orderId());
        report.setStatus("CONFIRMED");
        touch(report);
    }

    @Transactional
    public void onCompleted(TicketOrderCompleted event) {
        OrderReport report = upsert(event.orderId());
        report.setStatus("COMPLETED");
        if (event.ticketIds() != null) {
            report.setTicketCount(event.ticketIds().size());
        }
        touch(report);
    }

    @Transactional
    public void onCancelled(TicketOrderCancelled event) {
        OrderReport report = upsert(event.orderId());
        report.setStatus("CANCELLED");
        touch(report);
    }

    @Transactional
    public void onPaymentAuthorized(PaymentAuthorized event) {
        OrderReport report = upsert(event.orderId());
        report.setPaymentStatus("AUTHORIZED");
        report.setPaidAt(toLocal(event.occurredAt()));
        report.setProviderTransactionId(event.providerTransactionId());
        touch(report);
    }

    @Transactional
    public void onPaymentFailed(PaymentFailed event) {
        OrderReport report = upsert(event.orderId());
        report.setPaymentStatus("FAILED");
        touch(report);
    }

    @Transactional
    public void onTicketIssued(TicketIssued event) {
        OrderReport report = upsert(event.orderId());
        if (event.ticketIds() != null) {
            report.setTicketCount(event.ticketIds().size());
        }
        touch(report);
    }

    @Transactional
    public void onTicketRefunded(TicketRefunded event) {
        OrderReport report = upsert(event.orderId());
        report.setRefundedAmount(report.getRefundedAmount().add(event.amount()));
        touch(report);
    }

    private OrderReport upsert(UUID orderId) {
        return orderReportRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    OrderReport report = new OrderReport();
                    report.setOrderId(orderId);
                    return report;
                });
    }

    private void touch(OrderReport report) {
        report.setUpdatedAt(toLocal(Instant.now()));
        orderReportRepository.save(report);
    }

    private static LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
