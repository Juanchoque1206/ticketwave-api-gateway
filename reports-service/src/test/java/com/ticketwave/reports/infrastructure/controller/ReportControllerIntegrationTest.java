package com.ticketwave.reports.infrastructure.controller;

import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.TicketOrderCompleted;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.reports.application.ReportQueryService;
import com.ticketwave.reports.domain.projection.AppUserProjection;
import com.ticketwave.reports.domain.projection.AppUserProjectionRepository;
import com.ticketwave.reports.domain.projection.EventProjection;
import com.ticketwave.reports.domain.projection.EventProjectionRepository;
import com.ticketwave.reports.domain.projection.NotificationProjection;
import com.ticketwave.reports.domain.projection.NotificationProjectionRepository;
import com.ticketwave.reports.domain.projection.PaymentProjection;
import com.ticketwave.reports.domain.projection.PaymentProjectionRepository;
import com.ticketwave.reports.domain.projection.TicketProjection;
import com.ticketwave.reports.domain.projection.TicketProjectionRepository;
import com.ticketwave.reports.infrastructure.bus.InMemoryEventBus;
import com.ticketwave.reports.infrastructure.dto.report.OrderReportResponse;
import com.ticketwave.reports.infrastructure.dto.report.SalesByEventResponse;
import com.ticketwave.reports.infrastructure.dto.report.SalesOverviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the full reporting pipeline end-to-end (event projection plus the
 * read-only queries backing the /api/reports endpoints), mirroring the
 * service-level integration test style used across the platform.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Autowired
    private InMemoryEventBus eventBus;
    @Autowired
    private EventProjectionRepository eventProjectionRepository;
    @Autowired
    private AppUserProjectionRepository appUserProjectionRepository;
    @Autowired
    private PaymentProjectionRepository paymentProjectionRepository;
    @Autowired
    private TicketProjectionRepository ticketProjectionRepository;
    @Autowired
    private NotificationProjectionRepository notificationProjectionRepository;
    @Autowired
    private com.ticketwave.reports.domain.order.OrderReportRepository orderReportRepository;
    @Autowired
    private ReportQueryService reportQueryService;

    @BeforeEach
    void seed() {
        eventBus.clear();
        orderReportRepository.deleteAll();
        ticketProjectionRepository.deleteAll();
        paymentProjectionRepository.deleteAll();
        notificationProjectionRepository.deleteAll();
        appUserProjectionRepository.deleteAll();
        eventProjectionRepository.deleteAll();
        eventProjectionRepository.save(new EventProjection(EVENT_ID, "Summer Music Festival", "Artist",
                "Madrid", LocalDateTime.now().plusMonths(1), new BigDecimal("100.00"), 100, 0,
                "PUBLISHED", LocalDateTime.now()));
        appUserProjectionRepository.save(new AppUserProjection(USER_ID, "buyer", "buyer@example.com",
                "Buyer One", "Madrid", "USER", true));

        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                2, new BigDecimal("200.00"), BigDecimal.ZERO));
        eventBus.publish(new PaymentAuthorized(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID,
                new BigDecimal("200.00"), "TXN-123"));
        eventBus.publish(new TicketOrderConfirmed(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                new BigDecimal("200.00")));
        eventBus.publish(new TicketOrderCompleted(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                List.of(UUID.randomUUID()), new BigDecimal("200.00")));

        paymentProjectionRepository.save(new PaymentProjection(UUID.randomUUID(), ORDER_ID, "STRIPE", "COMPLETED",
                new BigDecimal("200.00"), "TXN-123", LocalDateTime.now()));
        ticketProjectionRepository.save(new TicketProjection(UUID.randomUUID(), "QR-0001", ORDER_ID, USER_ID,
                EVENT_ID, new BigDecimal("100.00"), "A12", "EMITTED", LocalDateTime.now(), null, null));
        notificationProjectionRepository.save(new NotificationProjection(UUID.randomUUID(), USER_ID,
                "ORDER_CONFIRMED", "EMAIL", "Your order is confirmed", "Enjoy the show", false, LocalDateTime.now()));
    }

    @Test
    void overview_returnsAggregatedSales() {
        SalesOverviewResponse overview = reportQueryService.salesOverview(Instant.EPOCH, Instant.now().plusSeconds(3600));
        assertEquals(1, overview.orders());
        assertEquals(1, overview.ticketsSold());
        assertEquals(new BigDecimal("200.00"), overview.netRevenue());
    }

    @Test
    void salesByEvent_groupsByEvent() {
        List<SalesByEventResponse> byEvent =
                reportQueryService.salesByEvent(Instant.EPOCH, Instant.now().plusSeconds(3600));
        assertEquals(1, byEvent.size());
        assertEquals("Summer Music Festival", byEvent.get(0).eventName());
        assertEquals(1, byEvent.get(0).tickets());
    }

    @Test
    void orderDetail_returnsMaterializedState() {
        OrderReportResponse order = reportQueryService.getOrder(ORDER_ID);
        assertEquals("COMPLETED", order.status());
        assertEquals("TXN-123", order.providerTransactionId());
        assertEquals(new BigDecimal("200.00"), order.totalAmount());

        assertThrows(com.ticketwave.reports.infrastructure.exception.ResourceNotFoundException.class,
                () -> reportQueryService.getOrder(UUID.randomUUID()));
    }

    @Test
    void readOnlyQueries_returnProjectedData() {
        assertEquals("STRIPE", reportQueryService.listPayments(null, Instant.EPOCH, Instant.now().plusSeconds(3600)).get(0).provider());
        assertEquals("QR-0001", reportQueryService.listTickets(null, Instant.EPOCH, Instant.now().plusSeconds(3600)).get(0).qrCode());
        assertEquals("buyer", reportQueryService.listTickets(null, Instant.EPOCH, Instant.now().plusSeconds(3600)).get(0).username());
        assertEquals("Your order is confirmed",
                reportQueryService.listNotifications(Instant.EPOCH, Instant.now().plusSeconds(3600)).get(0).subject());
    }
}
