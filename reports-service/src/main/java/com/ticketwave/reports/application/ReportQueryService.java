package com.ticketwave.reports.application;

import com.ticketwave.reports.domain.order.OrderReport;
import com.ticketwave.reports.domain.order.OrderReportRepository;
import com.ticketwave.reports.domain.projection.AppUserProjectionRepository;
import com.ticketwave.reports.domain.projection.EventProjectionRepository;
import com.ticketwave.reports.domain.projection.NotificationProjection;
import com.ticketwave.reports.domain.projection.NotificationProjectionRepository;
import com.ticketwave.reports.domain.projection.PaymentProjection;
import com.ticketwave.reports.domain.projection.PaymentProjectionRepository;
import com.ticketwave.reports.domain.projection.TicketProjection;
import com.ticketwave.reports.domain.projection.TicketProjectionRepository;
import com.ticketwave.reports.infrastructure.dto.report.NotificationReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.OrderReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.PaymentReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.SalesByEventResponse;
import com.ticketwave.reports.infrastructure.dto.report.SalesOverviewResponse;
import com.ticketwave.reports.infrastructure.dto.report.TicketReportResponse;
import com.ticketwave.reports.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only aggregation service backing the /api/reports endpoints. Order data
 * comes from the materialized {@code order_reports} read model; payments,
 * tickets, notifications and events are queried directly against the analytics
 * read replica of the monolith database.
 */
@Service
public class ReportQueryService {

    private static final List<String> PAID_STATUSES = List.of("CONFIRMED", "COMPLETED");

    private final OrderReportRepository orderReportRepository;
    private final PaymentProjectionRepository paymentProjectionRepository;
    private final TicketProjectionRepository ticketProjectionRepository;
    private final NotificationProjectionRepository notificationProjectionRepository;
    private final EventProjectionRepository eventProjectionRepository;
    private final AppUserProjectionRepository appUserProjectionRepository;

    public ReportQueryService(OrderReportRepository orderReportRepository,
                              PaymentProjectionRepository paymentProjectionRepository,
                              TicketProjectionRepository ticketProjectionRepository,
                              NotificationProjectionRepository notificationProjectionRepository,
                              EventProjectionRepository eventProjectionRepository,
                              AppUserProjectionRepository appUserProjectionRepository) {
        this.orderReportRepository = orderReportRepository;
        this.paymentProjectionRepository = paymentProjectionRepository;
        this.ticketProjectionRepository = ticketProjectionRepository;
        this.notificationProjectionRepository = notificationProjectionRepository;
        this.eventProjectionRepository = eventProjectionRepository;
        this.appUserProjectionRepository = appUserProjectionRepository;
    }

    @Transactional(readOnly = true)
    public SalesOverviewResponse salesOverview(Instant from, Instant to) {
        List<OrderReport> orders = orderReportRepository.findByReservedAtBetween(toLocal(from), toLocal(to));
        List<OrderReport> paid = orders.stream().filter(o -> PAID_STATUSES.contains(o.getStatus())).toList();
        BigDecimal grossRevenue = paid.stream()
                .map(o -> o.getTotalAmount().subtract(o.getDiscountAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = paid.stream()
                .map(OrderReport::getRefundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue = grossRevenue.subtract(refunds);
        long ticketsSold = paid.stream().mapToLong(OrderReport::getTicketCount).sum();
        long cancelled = orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count();
        BigDecimal avgOrderValue = paid.isEmpty() ? BigDecimal.ZERO
                : netRevenue.divide(BigDecimal.valueOf(paid.size()), 2, RoundingMode.HALF_UP);
        return new SalesOverviewResponse(grossRevenue, netRevenue, refunds,
                paid.size(), cancelled, ticketsSold, avgOrderValue);
    }

    @Transactional(readOnly = true)
    public List<SalesByEventResponse> salesByEvent(Instant from, Instant to) {
        List<OrderReport> paid = orderReportRepository.findByReservedAtBetween(toLocal(from), toLocal(to)).stream()
                .filter(o -> PAID_STATUSES.contains(o.getStatus()))
                .toList();
        Map<UUID, List<OrderReport>> byEvent = paid.stream()
                .collect(Collectors.groupingBy(OrderReport::getEventId));
        return byEvent.entrySet().stream().map(entry -> {
            UUID eventId = entry.getKey();
            List<OrderReport> rows = entry.getValue();
            BigDecimal revenue = rows.stream()
                    .map(o -> o.getTotalAmount().subtract(o.getDiscountAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal refunds = rows.stream()
                    .map(OrderReport::getRefundedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long tickets = rows.stream().mapToLong(OrderReport::getTicketCount).sum();
            String name = rows.stream().map(OrderReport::getEventName)
                    .filter(n -> n != null).findFirst()
                    .orElseGet(() -> eventProjectionRepository.findById(eventId)
                            .map(e -> e.getName()).orElse("unknown"));
            String city = eventProjectionRepository.findById(eventId).map(e -> e.getCity()).orElse(null);
            return new SalesByEventResponse(eventId, name, city, revenue, refunds, rows.size(), tickets);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderReportResponse> listOrders(UUID userId, String status, Instant from, Instant to) {
        LocalDateTime f = toLocal(from);
        LocalDateTime t = toLocal(to);
        List<OrderReport> rows = userId != null
                ? orderReportRepository.findByUserIdAndReservedAtBetween(userId, f, t)
                : orderReportRepository.findByReservedAtBetween(f, t);
        if (status != null && !status.isBlank()) {
            rows = rows.stream().filter(o -> status.equals(o.getStatus())).toList();
        }
        return rows.stream().map(this::toOrderResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderReportResponse getOrder(UUID orderId) {
        OrderReport report = orderReportRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order report not found for id " + orderId));
        return toOrderResponse(report);
    }

    @Transactional(readOnly = true)
    public List<PaymentReportResponse> listPayments(String status, Instant from, Instant to) {
        LocalDateTime f = toLocal(from);
        LocalDateTime t = toLocal(to);
        List<PaymentProjection> rows = status != null && !status.isBlank()
                ? paymentProjectionRepository.findByStatusInAndPaidAtBetween(List.of(status), f, t)
                : paymentProjectionRepository.findByPaidAtBetween(f, t);
        return rows.stream()
                .map(p -> new PaymentReportResponse(p.getId(), p.getOrderId(), p.getProvider(), p.getStatus(),
                        p.getAmount(), p.getProviderTransactionId(), p.getPaidAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketReportResponse> listTickets(String status, Instant from, Instant to) {
        LocalDateTime f = toLocal(from);
        LocalDateTime t = toLocal(to);
        List<TicketProjection> rows = status != null && !status.isBlank()
                ? ticketProjectionRepository.findByStatusInAndIssuedAtBetween(List.of(status), f, t)
                : ticketProjectionRepository.findByIssuedAtBetween(f, t);
        return rows.stream().map(ticket -> {
            String username = appUserProjectionRepository.findById(ticket.getUserId()).map(u -> u.getUsername()).orElse(null);
            String eventName = eventProjectionRepository.findById(ticket.getEventId()).map(e -> e.getName()).orElse(null);
            return new TicketReportResponse(ticket.getId(), ticket.getQrCode(), ticket.getOrderId(), ticket.getUserId(), username,
                    ticket.getEventId(), eventName, ticket.getPrice(), ticket.getSeat(), ticket.getStatus(),
                    ticket.getIssuedAt(), ticket.getValidatedAt(), ticket.getRefundedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationReportResponse> listNotifications(Instant from, Instant to) {
        List<NotificationProjection> rows = notificationProjectionRepository.findByCreatedAtBetween(toLocal(from), toLocal(to));
        return rows.stream().map(n -> {
            String username = appUserProjectionRepository.findById(n.getUserId()).map(u -> u.getUsername()).orElse(null);
            return new NotificationReportResponse(n.getId(), n.getUserId(), username, n.getType(), n.getChannel(),
                    n.getSubject(), n.getBody(), n.isRead(), n.getCreatedAt());
        }).toList();
    }

    private OrderReportResponse toOrderResponse(OrderReport o) {
        return new OrderReportResponse(o.getOrderId(), o.getUserId(), o.getEventId(), o.getEventName(),
                o.getQuantity(), o.getTotalAmount(), o.getDiscountAmount(), o.getStatus(),
                o.getReservedAt(), o.getUpdatedAt(), o.getPaidAt(), o.getPaymentStatus(),
                o.getProviderTransactionId(), o.getTicketCount(), o.getRefundedAmount());
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
