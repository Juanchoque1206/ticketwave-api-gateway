package com.ticketwave.infrastructure.controller;

import com.ticketwave.application.ConfirmOrderUseCase;
import com.ticketwave.application.PaymentService;
import com.ticketwave.infrastructure.dto.payment.CreatePaymentRequest;
import com.ticketwave.infrastructure.dto.payment.PaymentResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Integration with external payment providers (Stripe/PayPal)")
public class PaymentController {

    private final PaymentService paymentService;
    private final ConfirmOrderUseCase confirmOrderUseCase;

    public PaymentController(PaymentService paymentService, ConfirmOrderUseCase confirmOrderUseCase) {
        this.paymentService = paymentService;
        this.confirmOrderUseCase = confirmOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmOrderUseCase.confirm(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getForOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getForOrder(orderId));
    }
}