package com.ticketwave.domain.saga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hexagonal port for persisting saga state. The Redis adapter stores snapshots
 * with a TTL so finished or aborted sagas are cleaned up automatically while
 * running sagas survive an orchestrator restart.
 */
public interface SagaStateRepository {

    void save(SagaState state);

    Optional<SagaState> findById(UUID sagaId);

    Optional<SagaState> findByOrderId(UUID orderId);

    List<SagaState> findAll();

    void deleteById(UUID sagaId);
}