package com.ticketwave.application;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.commands.RefundPaymentCommand;
import com.ticketwave.domain.events.TicketRefunded;
import com.ticketwave.domain.payment.Payment;
import com.ticketwave.domain.payment.PaymentProvider;
import com.ticketwave.domain.payment.PaymentStatus;
import com.ticketwave.infrastructure.dto.payment.PaymentResponse;
import com.ticketwave.infrastructure.exception.PaymentException;
import com.ticketwave.infrastructure.exception.ResourceNotFoundException;
import com.ticketwave.domain.payment.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventBus eventBus;

    public PaymentService(PaymentRepository paymentRepository,
                          EventBus eventBus,
                          CommandBus commandBus) {
        this.paymentRepository = paymentRepository;
        this.eventBus = eventBus;
        commandBus.subscribe(RefundPaymentCommand.class, this::refundPayment);
    }

    /**
     * Creates and settles a payment for an order. The order aggregate lives in
     * the ticketorder-service, so the charged amount is supplied by the caller
     * (the saga command carries it; the REST path resolves it from the order
     * service) and only the scalar orderId is persisted here.
     */
    @Transactional
    public PaymentResponse create(UUID orderId, PaymentProvider provider, BigDecimal amount) {
        if (provider == null) {
            throw new PaymentException("A payment provider is required");
        }

        paymentRepository.findByOrderId(orderId)
                .filter(existing -> existing.getStatus() == PaymentStatus.SUCCEEDED)
                .ifPresent(existing -> {
                    throw new PaymentException("Payment already completed for this order");
                });

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setProvider(provider);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment = paymentRepository.save(payment);

        boolean succeeded = payWithProvider(provider, payment.getAmount());
        if (!succeeded) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Payment failed with provider " + provider);
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setProviderTransactionId("TXN-" + UUID.randomUUID());
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(RefundPaymentCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        eventBus.publish(new TicketRefunded(UUID.randomUUID(), Instant.now(),
                null, command.orderId(), command.userId(), command.amount()));
        return toResponse(payment);
    }

    private boolean payWithProvider(PaymentProvider provider, BigDecimal amount) {
        return switch (provider) {
            case STRIPE -> simulateProviderCall("stripe", amount);
            case PAYPAL -> simulateProviderCall("paypal", amount);
        };
    }

    private boolean simulateProviderCall(String providerName, BigDecimal amount) {
        // Placeholder for real Stripe/PayPal SDK integration.
        return true;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getForOrder(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getProvider(),
                payment.getStatus(), payment.getAmount(), payment.getProviderTransactionId(),
                payment.getPaidAt(), null);
    }
}
