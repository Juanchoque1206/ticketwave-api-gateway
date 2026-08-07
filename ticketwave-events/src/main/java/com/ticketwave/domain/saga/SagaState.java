package com.ticketwave.domain.saga;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable snapshot of a running order saga. State can only be changed by the
 * orchestrator through the copy-on-write methods (progress / complete / fail /
 * compensate), so every persisted mutation is the result of a single decision.
 */
public final class SagaState {

    private final UUID sagaId;
    private final UUID orderId;
    private final UUID userId;
    private final UUID eventId;
    private final int quantity;
    private final BigDecimal total;
    private final SagaStatus status;
    private final SagaStep currentStep;
    private final String error;
    private final Map<String, String> payload;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SagaState(UUID sagaId, UUID orderId, UUID userId, UUID eventId, int quantity,
                     BigDecimal total, SagaStatus status, SagaStep currentStep, String error,
                     Map<String, String> payload, Instant createdAt, Instant updatedAt) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.quantity = quantity;
        this.total = total;
        this.status = status;
        this.currentStep = currentStep;
        this.error = error;
        this.payload = Map.copyOf(payload);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SagaState start(UUID orderId, UUID userId, UUID eventId, int quantity, BigDecimal total) {
        Instant now = Instant.now();
        return new SagaState(UUID.randomUUID(), orderId, userId, eventId, quantity, total,
                SagaStatus.CREATED, SagaStep.ORDER_CREATED, null, Map.of(), now, now);
    }

    public SagaState progress(SagaStep step) {
        return copy(SagaStatus.RUNNING, step, null, payload);
    }

    public SagaState complete() {
        return copy(SagaStatus.COMPLETED, SagaStep.COMPLETED, null, payload);
    }

    public SagaState fail(String reason) {
        return copy(SagaStatus.FAILED, SagaStep.FAILED, reason, payload);
    }

    public SagaState compensate() {
        return copy(SagaStatus.COMPENSATING, SagaStep.COMPENSATING, null, payload);
    }

    public SagaState compensated() {
        return copy(SagaStatus.COMPENSATED, SagaStep.COMPENSATED, null, payload);
    }

    public SagaState withPayload(String key, String value) {
        Map<String, String> next = new HashMap<>(payload);
        next.put(key, value);
        return copy(status, currentStep, error, next);
    }

    private SagaState copy(SagaStatus newStatus, SagaStep newStep, String newError, Map<String, String> newPayload) {
        return new SagaState(sagaId, orderId, userId, eventId, quantity, total,
                newStatus, newStep, newError, newPayload, createdAt, Instant.now());
    }

    public UUID sagaId() {
        return sagaId;
    }

    public UUID orderId() {
        return orderId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID eventId() {
        return eventId;
    }

    public int quantity() {
        return quantity;
    }

    public BigDecimal total() {
        return total;
    }

    public SagaStatus status() {
        return status;
    }

    public SagaStep currentStep() {
        return currentStep;
    }

    public String error() {
        return error;
    }

    public Map<String, String> payload() {
        return payload;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}