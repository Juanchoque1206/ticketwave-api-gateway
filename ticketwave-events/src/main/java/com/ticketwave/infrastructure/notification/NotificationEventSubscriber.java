package com.ticketwave.infrastructure.notification;

import com.ticketwave.application.NotificationService;
import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.commands.NotifyOrderCommand;
import com.ticketwave.domain.events.NotificationFailed;
import com.ticketwave.domain.events.NotificationSent;
import com.ticketwave.domain.events.PaymentFailed;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.domain.events.TicketRefunded;
import com.ticketwave.domain.notification.Notification;
import com.ticketwave.domain.notification.NotificationType;
import com.ticketwave.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscriber adapter that turns order lifecycle domain events into user
 * notifications, keeping the notification channel decoupled from the
 * application services that trigger it.
 * <p>
 * The purchase-completed notification is an orchestrated saga step: it handles
 * the NotifyOrderCommand sent by the orchestrator and answers with
 * NotificationSent / NotificationFailed so the saga can finish (a failed
 * notification does not compensate the purchase).
 */
@Component
public class NotificationEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventSubscriber.class);

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EventBus eventBus;

    public NotificationEventSubscriber(EventBus eventBus,
                                       CommandBus commandBus,
                                       UserRepository userRepository,
                                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.eventBus = eventBus;
        eventBus.subscribe(TicketOrderCreated.class, this::onCreated);
        eventBus.subscribe(PaymentFailed.class, this::onPaymentFailed);
        eventBus.subscribe(TicketOrderCancelled.class, this::onCancelled);
        eventBus.subscribe(TicketRefunded.class, this::onRefunded);
        commandBus.subscribe(NotifyOrderCommand.class, this::onNotifyOrder);
    }

    private void onCreated(TicketOrderCreated event) {
        notify(event.userId(), NotificationType.ORDER_CONFIRMATION,
                "Reservation created",
                "Your reservation " + event.orderId() + " is pending payment.");
    }

    private void onNotifyOrder(NotifyOrderCommand command) {
        try {
            userRepository.findById(command.userId()).ifPresentOrElse(
                    user -> {
                        Notification notification = notificationService.send(user, NotificationType.PAYMENT_RECEIVED,
                                "Purchase completed",
                                "Your tickets for order " + command.orderId() + " are ready.");
                        eventBus.publish(new NotificationSent(UUID.randomUUID(), Instant.now(),
                                command.orderId(), command.userId(), notification.getId()));
                    },
                    () -> eventBus.publish(new NotificationFailed(UUID.randomUUID(), Instant.now(),
                            command.orderId(), command.userId(), "User not found")));
        } catch (Exception ex) {
            log.error("Notification step failed for order {}", command.orderId(), ex);
            eventBus.publish(new NotificationFailed(UUID.randomUUID(), Instant.now(),
                    command.orderId(), command.userId(), ex.getMessage()));
        }
    }

    private void onPaymentFailed(PaymentFailed event) {
        notify(event.userId(), NotificationType.PAYMENT_RECEIVED,
                "Payment failed",
                "Payment for order " + event.orderId() + " failed: " + event.reason());
    }

    private void onCancelled(TicketOrderCancelled event) {
        notify(event.userId(), NotificationType.ORDER_CANCELLED,
                "Order cancelled",
                "Your order " + event.orderId() + " has been cancelled.");
    }

    private void onRefunded(TicketRefunded event) {
        notify(event.userId(), NotificationType.ORDER_CANCELLED,
                "Refund processed",
                "Refund of " + event.amount() + " for ticket " + event.ticketId() + " was processed.");
    }

    private void notify(java.util.UUID userId, NotificationType type, String subject, String body) {
        userRepository.findById(userId).ifPresentOrElse(
                user -> notificationService.send(user, type, subject, body),
                () -> log.warn("Notification skipped, user {} not found", userId));
    }
}
