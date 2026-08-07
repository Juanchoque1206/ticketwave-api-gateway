package com.ticketwave;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.event.EventRepository;
import com.ticketwave.domain.event.EventStatus;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.notification.NotificationRepository;
import com.ticketwave.domain.payment.Payment;
import com.ticketwave.domain.payment.PaymentRepository;
import com.ticketwave.domain.payment.PaymentStatus;
import com.ticketwave.domain.ticket.Ticket;
import com.ticketwave.domain.ticket.TicketRepository;
import com.ticketwave.domain.user.AppUser;
import com.ticketwave.domain.user.Role;
import com.ticketwave.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the full purchase saga offline: a TicketOrderCreated event published by
 * the ticketorder-service triggers Payment -&gt; Ticket issuance -&gt;
 * Notification through the in-memory bus, with the order aggregate replaced by
 * the scalar orderId carried by the events/commands.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketOrderFlowIntegrationTest {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void orderCreatedEvent_drivesSagaEndToEnd() {
        AppUser user = createUser();
        Event event = createEvent();
        UUID orderId = UUID.randomUUID();
        BigDecimal total = event.getBasePrice().multiply(BigDecimal.valueOf(2));

        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(),
                orderId, user.getId(), event.getId(), 2, total, BigDecimal.ZERO));

        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        assertEquals(2, tickets.size());
        tickets.forEach(ticket -> assertEquals(user.getId(), ticket.getUserId()));

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        assertNotNull(payment, "saga should have settled a payment");
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertEquals(total, payment.getAmount());

        assertTrue(!notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty(),
                "order lifecycle notifications expected");
    }

    private AppUser createUser() {
        AppUser user = new AppUser();
        user.setUsername("tester-" + UUID.randomUUID());
        user.setEmail("tester-" + UUID.randomUUID() + "@mail.com");
        user.setPassword("$2a$10$dummyhash");
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private Event createEvent() {
        Event event = new Event();
        event.setName("Test Event");
        event.setCity("Lima");
        event.setVenue("Test Venue");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setBasePrice(new BigDecimal("100.00"));
        event.setTotalCapacity(100);
        event.setStatus(EventStatus.PUBLISHED);
        return eventRepository.save(event);
    }
}
