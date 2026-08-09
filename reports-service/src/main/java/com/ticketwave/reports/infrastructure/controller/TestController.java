package com.ticketwave.reports.infrastructure.controller;

import com.ticketwave.reports.application.ReportQueryService;
import com.ticketwave.reports.infrastructure.dto.report.TicketReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Test-only endpoint with no security validation, used to verify the service
 * is reachable without a JWT. Not exposed through Kong in production.
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private final ReportQueryService reportQueryService;

    public TestController(ReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    @GetMapping
    public Map<String, Object> test() {
        return Map.of(
                "service", "reports-service",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/tickets")
    public List<TicketReportResponse> listTickets() {
        return reportQueryService.listTickets(null, Instant.EPOCH, Instant.now().plusSeconds(86400));
    }
}
