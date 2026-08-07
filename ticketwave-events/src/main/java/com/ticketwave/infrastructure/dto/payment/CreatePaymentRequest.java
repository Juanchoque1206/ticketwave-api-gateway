package com.ticketwave.infrastructure.dto.payment;

import com.ticketwave.domain.payment.PaymentProvider;

import java.util.UUID;

public record CreatePaymentRequest(
        UUID orderId,
        PaymentProvider provider
) {
}