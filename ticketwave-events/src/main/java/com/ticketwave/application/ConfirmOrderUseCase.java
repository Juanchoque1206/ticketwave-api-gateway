package com.ticketwave.application;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.commands.ProcessPaymentCommand;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.PaymentFailed;
import com.ticketwave.domain.payment.PaymentProvider;
import com.ticketwave.domain.payment.PaymentStatus;
import com.ticketwave.infrastructure.dto.payment.CreatePaymentRequest;
import com.ticketwave.infrastructure.dto.payment.PaymentResponse;
import com.ticketwave.infrastructure.order.OrderInfo;
import com.ticketwave.infrastructure.order.OrderInfoClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Executes the payment step of the saga: processes a ProcessPaymentCommand,
 * charges the order and publishes PaymentAuthorized (or PaymentFailed so the
 * orchestrator can compensate). Registered both as the command handler and as
 * the target of the legacy REST confirm endpoint.
 * <p>
 * The order aggregate lives in the ticketorder-service. The saga always carries
 * the amount (so the payment step stays fully asynchronous and offline-safe);
 * only the legacy REST path resolves the total through {@link OrderInfoClient}.
 */
@Service
public class ConfirmOrderUseCase {

    private final PaymentService paymentService;
    private final EventBus eventBus;
    private final OrderInfoClient orderInfoClient;
    private final TransactionTemplate transactionTemplate;

    public ConfirmOrderUseCase(PaymentService paymentService,
                               EventBus eventBus,
                               OrderInfoClient orderInfoClient,
                               CommandBus commandBus,
                               TransactionTemplate transactionTemplate) {
        this.paymentService = paymentService;
        this.eventBus = eventBus;
        this.orderInfoClient = orderInfoClient;
        this.transactionTemplate = transactionTemplate;
        commandBus.subscribe(ProcessPaymentCommand.class, this::processPayment);
    }

    @Transactional
    public PaymentResponse confirm(CreatePaymentRequest request) {
        return processPayment(new ProcessPaymentCommand(UUID.randomUUID(), Instant.now(),
                request.orderId(), request.provider().name(), null));
    }

    public PaymentResponse processPayment(ProcessPaymentCommand command) {
        return transactionTemplate.execute(status -> doProcessPayment(command));
    }

    private PaymentResponse doProcessPayment(ProcessPaymentCommand command) {
        UUID userId = null;
        BigDecimal total = command.amount();
        if (total == null) {
            OrderInfo order = orderInfoClient.getOrder(command.orderId());
            total = order.totalAmount();
            userId = order.userId();
        }

        try {
            PaymentResponse payment = paymentService.create(
                    command.orderId(), PaymentProvider.valueOf(command.provider()), total);
            eventBus.publish(new PaymentAuthorized(UUID.randomUUID(), Instant.now(),
                    command.orderId(), userId, payment.amount(), payment.providerTransactionId()));
            return payment;
        } catch (Exception ex) {
            eventBus.publish(new PaymentFailed(UUID.randomUUID(), Instant.now(),
                    command.orderId(), userId, total, ex.getMessage()));
            return failedResponse(command, total);
        }
    }

    private PaymentResponse failedResponse(ProcessPaymentCommand command, BigDecimal amount) {
        return new PaymentResponse(null, command.orderId(),
                PaymentProvider.valueOf(command.provider()), PaymentStatus.FAILED, amount, null, null, null);
    }
}
