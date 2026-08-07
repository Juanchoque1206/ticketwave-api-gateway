package com.ticketwave.reports.infrastructure.controller;

import com.ticketwave.reports.application.ReportQueryService;
import com.ticketwave.reports.infrastructure.dto.report.NotificationReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.OrderReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.PaymentReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.SalesByEventResponse;
import com.ticketwave.reports.infrastructure.dto.report.SalesOverviewResponse;
import com.ticketwave.reports.infrastructure.dto.report.TicketReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Read-only reporting endpoints. Exposed through Kong at /api/reports/**.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportQueryService reportQueryService;

    public ReportController(ReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    @GetMapping("/sales/overview")
    public SalesOverviewResponse salesOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportQueryService.salesOverview(from(from), to(to));
    }

    @GetMapping("/sales/by-event")
    public List<SalesByEventResponse> salesByEvent(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportQueryService.salesByEvent(from(from), to(to));
    }

    @GetMapping("/orders")
    public List<OrderReportResponse> listOrders(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportQueryService.listOrders(userId, status, from(from), to(to));
    }

    @GetMapping("/orders/{orderId}")
    public OrderReportResponse getOrder(@PathVariable UUID orderId) {
        return reportQueryService.getOrder(orderId);
    }

    @GetMapping("/payments")
    public List<PaymentReportResponse> listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportQueryService.listPayments(status, from(from), to(to));
    }

    @GetMapping("/tickets")
    public List<TicketReportResponse> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportQueryService.listTickets(status, from(from), to(to));
    }

    @GetMapping("/notifications")
    public List<NotificationReportResponse> listNotifications(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportQueryService.listNotifications(from(from), to(to));
    }

    private static Instant from(Instant from) {
        return from != null ? from : Instant.EPOCH;
    }

    private static Instant to(Instant to) {
        return to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);
    }
}
