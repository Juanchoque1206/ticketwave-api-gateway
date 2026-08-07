package com.ticketwave.reports.infrastructure.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesByEventResponse(
        UUID eventId,
        String eventName,
        String city,
        BigDecimal revenue,
        BigDecimal refunds,
        long orders,
        long tickets) {
}
