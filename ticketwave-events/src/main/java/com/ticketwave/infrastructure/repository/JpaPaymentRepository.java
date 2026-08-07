package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.payment.Payment;
import com.ticketwave.domain.payment.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPaymentRepository extends PaymentRepository, JpaRepository<Payment, UUID> {
}