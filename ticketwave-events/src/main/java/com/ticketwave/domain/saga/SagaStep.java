package com.ticketwave.domain.saga;

public enum SagaStep {
    ORDER_CREATED,
    PAYMENT_PROCESSED,
    TICKETS_ISSUED,
    NOTIFICATION_SENT,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}