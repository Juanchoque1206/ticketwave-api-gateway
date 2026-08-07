package com.ticketwave.domain.payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findById(UUID id);

    Payment save(Payment payment);
}