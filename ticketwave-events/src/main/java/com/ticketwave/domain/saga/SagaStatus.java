package com.ticketwave.domain.saga;

public enum SagaStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}