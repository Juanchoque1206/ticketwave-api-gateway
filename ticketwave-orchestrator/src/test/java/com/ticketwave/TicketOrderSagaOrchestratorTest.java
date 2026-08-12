package com.ticketwave;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.commands.CancelTicketOrderCommand;
import com.ticketwave.domain.commands.IssueTicketCommand;
import com.ticketwave.domain.commands.NotifyOrderCommand;
import com.ticketwave.domain.commands.ProcessPaymentCommand;
import com.ticketwave.domain.commands.RefundPaymentCommand;
import com.ticketwave.domain.events.NotificationSent;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.TicketIssued;
import com.ticketwave.domain.events.TicketOrderCompleted;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.saga.SagaState;
import com.ticketwave.domain.saga.SagaStateRepository;
import com.ticketwave.domain.saga.SagaStatus;
import com.ticketwave.domain.saga.TicketOrderSagaOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the saga orchestrator offline through the in-memory bus and repository,
 * verifying that each lifecycle event advances the saga and publishes the next
 * command (the same sequence the RabbitMQ adapters would deliver in production).
 *
 * The orchestrator is a Spring bean that subscribes to the bus on startup, so it
 * is autowired here instead of being manually instantiated (a manual instance
 * would subscribe a second time and double every event).
 */
@SpringBootTest
@ActiveProfiles("test")
class TicketOrderSagaOrchestratorTest {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private CommandBus commandBus;
    @Autowired
    private SagaStateRepository sagaRepository;
    @Autowired
    private TicketOrderSagaOrchestrator orchestrator;

    private final List<Object> commands = new CopyOnWriteArrayList<>();

    @Test
    void orderCreated_drivesSagaToCompletion() {
        commandBus.subscribe(ProcessPaymentCommand.class, commands::add);
        commandBus.subscribe(IssueTicketCommand.class, commands::add);
        commandBus.subscribe(NotifyOrderCommand.class, commands::add);
        commandBus.subscribe(RefundPaymentCommand.class, commands::add);

        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        BigDecimal total = new BigDecimal("200.00");

        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(),
                orderId, userId, eventId, 2, total, BigDecimal.ZERO));

        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof ProcessPaymentCommand);
        assertEquals("STRIPE", ((ProcessPaymentCommand) commands.get(0)).provider());
        assertEquals(total, ((ProcessPaymentCommand) commands.get(0)).amount());

        eventBus.publish(new PaymentAuthorized(UUID.randomUUID(), Instant.now(),
                orderId, userId, total, "TXN-1"));

        assertEquals(2, commands.size());
        assertTrue(commands.get(1) instanceof IssueTicketCommand);
        assertEquals(2, ((IssueTicketCommand) commands.get(1)).quantity());

        eventBus.publish(new TicketIssued(UUID.randomUUID(), Instant.now(),
                orderId, userId, eventId, List.of(UUID.randomUUID(), UUID.randomUUID())));

        assertEquals(3, commands.size());
        assertTrue(commands.get(2) instanceof NotifyOrderCommand);

        eventBus.publish(new NotificationSent(UUID.randomUUID(), Instant.now(),
                orderId, userId, UUID.randomUUID()));

        SagaState state = sagaRepository.findByOrderId(orderId).orElseThrow();
        assertEquals(SagaStatus.COMPLETED, state.status());
    }

    @Test
    void paymentFailed_sendsCancelCompensation() {
        commandBus.subscribe(ProcessPaymentCommand.class, commands::add);
        commandBus.subscribe(IssueTicketCommand.class, commands::add);
        commandBus.subscribe(NotifyOrderCommand.class, commands::add);
        commandBus.subscribe(RefundPaymentCommand.class, commands::add);
        commandBus.subscribe(CancelTicketOrderCommand.class, commands::add);

        UUID orderId = UUID.randomUUID();
        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(),
                orderId, UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("200.00"), BigDecimal.ZERO));
        eventBus.publish(new com.ticketwave.domain.events.PaymentFailed(UUID.randomUUID(), Instant.now(),
                orderId, UUID.randomUUID(), new BigDecimal("200.00"), "insufficient funds"));

        SagaState state = sagaRepository.findByOrderId(orderId).orElseThrow();
        assertEquals(SagaStatus.FAILED, state.status());

        assertTrue(commands.stream().anyMatch(c -> c instanceof CancelTicketOrderCommand));
    }

    @Test
    void completedSaga_publishesTicketOrderCompleted() {
        List<Object> events = new CopyOnWriteArrayList<>();
        eventBus.subscribe(TicketOrderCompleted.class, events::add);

        commandBus.subscribe(ProcessPaymentCommand.class, commands::add);
        commandBus.subscribe(IssueTicketCommand.class, commands::add);
        commandBus.subscribe(NotifyOrderCommand.class, commands::add);

        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        eventBus.publish(new TicketOrderCreated(UUID.randomUUID(), Instant.now(),
                orderId, userId, UUID.randomUUID(), 1, new BigDecimal("100.00"), BigDecimal.ZERO));
        eventBus.publish(new PaymentAuthorized(UUID.randomUUID(), Instant.now(),
                orderId, userId, new BigDecimal("100.00"), "TXN-2"));
        eventBus.publish(new TicketIssued(UUID.randomUUID(), Instant.now(),
                orderId, userId, UUID.randomUUID(), List.of(UUID.randomUUID())));
        eventBus.publish(new NotificationSent(UUID.randomUUID(), Instant.now(),
                orderId, userId, UUID.randomUUID()));

        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof TicketOrderCompleted);
    }
}