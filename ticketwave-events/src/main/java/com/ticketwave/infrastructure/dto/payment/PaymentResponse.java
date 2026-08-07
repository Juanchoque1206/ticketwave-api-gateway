package com.ticketwave.infrastructure.dto.payment;

import com.ticketwave.domain.payment.PaymentProvider;
import com.ticketwave.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        PaymentProvider provider,
        PaymentStatus status,
        BigDecimal amount,
        String providerTransactionId,
        LocalDateTime paidAt,
        String checkoutUrl
) {
}