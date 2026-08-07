package com.ticketwave.domain.saga;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.commands.CancelTicketOrderCommand;
import com.ticketwave.domain.commands.IssueTicketCommand;
import com.ticketwave.domain.commands.NotifyOrderCommand;
import com.ticketwave.domain.commands.ProcessPaymentCommand;
import com.ticketwave.domain.commands.RefundPaymentCommand;
import com.ticketwave.domain.events.NotificationFailed;
import com.ticketwave.domain.events.NotificationSent;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.PaymentFailed;
import com.ticketwave.domain.events.TicketDeliveryFailed;
import com.ticketwave.domain.events.TicketIssued;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderCompleted;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.events.TicketRefunded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Central saga orchestrator for the ticket purchase workflow:
 * TicketOrder -&gt; Payment -&gt; Ticket Issuance -&gt; Notification.
 * <p>
 * It subscribes to domain events, advances the saga snapshot in the
 * SagaStateRepository and drives each step by sending commands through the
 * CommandBus. Every step transition is persisted before a command is sent so
 * that a crash can be resumed from Redis via {@link #recover()}.
 * <p>
 * Compensations: a failed payment cancels the order; a failed ticket delivery
 * refunds the payment; both converge on {@link SagaStatus#COMPENSATED}. A
 * failed notification is only logged and the saga still completes.
 */
@Component
public class TicketOrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TicketOrderSagaOrchestrator.class);

    private final EventBus eventBus;
    private final CommandBus commandBus;
    private final SagaStateRepository sagaRepository;

    public TicketOrderSagaOrchestrator(EventBus eventBus,
                                       CommandBus commandBus,
                                       SagaStateRepository sagaRepository) {
        this.eventBus = eventBus;
        this.commandBus = commandBus;
        this.sagaRepository = sagaRepository;
        eventBus.subscribe(TicketOrderCreated.class, this::onOrderCreated);
        eventBus.subscribe(PaymentAuthorized.class, this::onPaymentAuthorized);
        eventBus.subscribe(PaymentFailed.class, this::onPaymentFailed);
        eventBus.subscribe(TicketIssued.class, this::onTicketIssued);
        eventBus.subscribe(TicketDeliveryFailed.class, this::onTicketDeliveryFailed);
        eventBus.subscribe(NotificationSent.class, this::onNotificationSent);
        eventBus.subscribe(NotificationFailed.class, this::onNotificationFailed);
        eventBus.subscribe(TicketRefunded.class, this::onTicketRefunded);
        eventBus.subscribe(TicketOrderCancelled.class, this::onOrderCancelled);
    }

    public void onOrderCreated(TicketOrderCreated event) {
        if (sagaRepository.findByOrderId(event.orderId()).isPresent()) {
            return;
        }
        SagaState state = SagaState.start(event.orderId(), event.userId(), event.eventId(),
                event.quantity(), event.total());
        sagaRepository.save(state);
        commandBus.send(new ProcessPaymentCommand(UUID.randomUUID(), Instant.now(),
                event.orderId(), "STRIPE", event.total()));
    }

    public void onPaymentAuthorized(PaymentAuthorized event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            SagaState next = state.progress(SagaStep.PAYMENT_PROCESSED);
            sagaRepository.save(next);
            commandBus.send(new IssueTicketCommand(UUID.randomUUID(), Instant.now(),
                    next.orderId(), next.userId(), next.eventId(), next.quantity()));
        });
    }

    public void onPaymentFailed(PaymentFailed event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            sagaRepository.save(state.fail(event.reason()));
            commandBus.send(new CancelTicketOrderCommand(UUID.randomUUID(), Instant.now(),
                    state.orderId(), state.userId(), state.eventId(), state.quantity(), event.reason()));
        });
    }

    public void onTicketIssued(TicketIssued event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            SagaState next = state.withPayload("ticketIds", event.ticketIds().toString())
                    .progress(SagaStep.NOTIFICATION_SENT);
            sagaRepository.save(next);
            commandBus.send(new NotifyOrderCommand(UUID.randomUUID(), Instant.now(),
                    next.orderId(), next.userId(), next.eventId()));
        });
    }

    public void onNotificationSent(NotificationSent event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            sagaRepository.save(state.withPayload("notificationId", String.valueOf(event.notificationId())).complete());
            publishCompleted(state);
        });
    }

    public void onNotificationFailed(NotificationFailed event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            log.warn("Notification failed for order {}: {} (saga still completes)", state.orderId(), event.reason());
            sagaRepository.save(state.complete());
            publishCompleted(state);
        });
    }

    private void publishCompleted(SagaState state) {
        List<UUID> ticketIds = state.payload().getOrDefault("ticketIds", "").isEmpty()
                ? List.of()
                : Arrays.stream(state.payload().get("ticketIds")
                                .replace("[", "").replace("]", "").split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(UUID::fromString)
                        .toList();
        eventBus.publish(new TicketOrderCompleted(UUID.randomUUID(), Instant.now(),
                state.orderId(), state.userId(), state.eventId(), ticketIds, state.total()));
    }

    public void onTicketDeliveryFailed(TicketDeliveryFailed event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            sagaRepository.save(state.fail(event.reason()));
            commandBus.send(new RefundPaymentCommand(UUID.randomUUID(), Instant.now(),
                    state.orderId(), state.userId(), state.total(), event.reason()));
        });
    }

    public void onTicketRefunded(TicketRefunded event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            if (state.status() == SagaStatus.FAILED || state.status() == SagaStatus.COMPENSATING) {
                sagaRepository.save(state.compensated());
            }
        });
    }

    public void onOrderCancelled(TicketOrderCancelled event) {
        sagaRepository.findByOrderId(event.orderId()).ifPresent(state -> {
            if (state.status() == SagaStatus.FAILED || state.status() == SagaStatus.COMPENSATING) {
                sagaRepository.save(state.compensated());
            }
        });
    }

    /**
     * Resumes every interrupted saga: sagas that were waiting on a command are
     * re-driven from their persisted step. Used by the recovery job and by the
     * monitoring endpoint to resume after an orchestrator breakdown.
     */
    public void recover() {
        for (SagaState state : sagaRepository.findAll()) {
            if (state.status() == SagaStatus.CREATED || state.status() == SagaStatus.RUNNING) {
                resendStep(state);
            }
        }
    }

    public void recoverSaga(UUID sagaId) {
        sagaRepository.findById(sagaId).ifPresent(state -> {
            if (state.status() == SagaStatus.CREATED || state.status() == SagaStatus.RUNNING) {
                resendStep(state);
            }
        });
    }

    private void resendStep(SagaState state) {
        switch (state.currentStep()) {
            case ORDER_CREATED -> commandBus.send(new ProcessPaymentCommand(UUID.randomUUID(), Instant.now(),
                    state.orderId(), "STRIPE", state.total()));
            case PAYMENT_PROCESSED -> commandBus.send(new IssueTicketCommand(UUID.randomUUID(), Instant.now(),
                    state.orderId(), state.userId(), state.eventId(), state.quantity()));
            case NOTIFICATION_SENT -> commandBus.send(new NotifyOrderCommand(UUID.randomUUID(), Instant.now(),
                    state.orderId(), state.userId(), state.eventId()));
            default -> {
                // nothing to resume
            }
        }
    }
}