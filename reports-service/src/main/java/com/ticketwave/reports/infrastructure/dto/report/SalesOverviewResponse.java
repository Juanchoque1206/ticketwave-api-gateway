package com.ticketwave.reports.infrastructure.dto.report;

import java.math.BigDecimal;

public record SalesOverviewResponse(
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal refunds,
        long orders,
        long cancelledOrders,
        long ticketsSold,
        BigDecimal avgOrderValue) {
}
