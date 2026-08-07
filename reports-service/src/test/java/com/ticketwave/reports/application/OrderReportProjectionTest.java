package com.ticketwave.reports.application;

import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.TicketIssued;
import com.ticketwave.domain.events.TicketOrderCompleted;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.events.TicketRefunded;
import com.ticketwave.reports.domain.order.OrderReport;
import com.ticketwave.reports.domain.order.OrderReportRepository;
import com.ticketwave.reports.domain.projection.EventProjection;
import com.ticketwave.reports.domain.projection.EventProjectionRepository;
import com.ticketwave.reports.infrastructure.bus.InMemoryEventBus;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class OrderReportProjectionTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private InMemoryEventBus eventBus;
    @Autowired
    private OrderReportRepository orderReportRepository;
    @Autowired
    private EventProjectionRepository eventProjectionRepository;

    @BeforeEach
    void setUp() {
        eventBus.clear();
        orderReportRepository.deleteAll();
        eventProjectionRepository.deleteAll();
    }

    @Test
    void fullLifecycle_materializesOrderReport() {
        eventProjectionRepository.save(new EventProjection(EVENT_ID, "Summer Music Festival", "Artist",
                "Madrid", LocalDateTime.now().plusMonths(1), new BigDecimal("100.00"), 100, 0,
                "PUBLISHED", LocalDateTime.now()));

        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                2, new BigDecimal("200.00"), BigDecimal.ZERO));

        OrderReport created = orderReportRepository.findByOrderId(ORDER_ID).orElseThrow();
        assertEquals("PENDING", created.getStatus());
        assertEquals("Summer Music Festival", created.getEventName());
        assertEquals(2, created.getQuantity());
        assertEquals(new BigDecimal("200.00"), created.getTotalAmount());
        assertEquals(USER_ID, created.getUserId());

        eventBus.publish(new PaymentAuthorized(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID,
                new BigDecimal("200.00"), "TXN-123"));
        eventBus.publish(new TicketOrderConfirmed(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                new BigDecimal("200.00")));
        eventBus.publish(new TicketOrderCompleted(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                List.of(UUID.randomUUID(), UUID.randomUUID()), new BigDecimal("200.00")));
        eventBus.publish(new TicketIssued(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                List.of(UUID.randomUUID(), UUID.randomUUID())));

        OrderReport paid = orderReportRepository.findByOrderId(ORDER_ID).orElseThrow();
        assertEquals("COMPLETED", paid.getStatus());
        assertEquals("AUTHORIZED", paid.getPaymentStatus());
        assertEquals("TXN-123", paid.getProviderTransactionId());
        assertNotNull(paid.getPaidAt());
        assertEquals(2, paid.getTicketCount());

        eventBus.publish(new TicketRefunded(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), ORDER_ID, USER_ID,
                new BigDecimal("50.00")));

        OrderReport refunded = orderReportRepository.findByOrderId(ORDER_ID).orElseThrow();
        assertEquals(new BigDecimal("50.00"), refunded.getRefundedAmount());
    }

    @Test
    void cancellation_marksOrderCancelled() {
        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(), ORDER_ID, USER_ID, EVENT_ID,
                2, new BigDecimal("200.00"), BigDecimal.ZERO));
        eventBus.publish(new com.ticketwave.domain.events.TicketOrderCancelled(UUID.randomUUID(), Instant.now(),
                ORDER_ID, USER_ID, EVENT_ID, 2));

        OrderReport report = orderReportRepository.findByOrderId(ORDER_ID).orElseThrow();
        assertEquals("CANCELLED", report.getStatus());
    }
}
