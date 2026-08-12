package com.ticketwave.application;

import com.ticketwave.domain.saga.TicketOrderSagaOrchestrator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically resumes interrupted sagas: any saga persisted as CREATED or
 * RUNNING is re-driven from its current step so a workflow survives an
 * orchestrator breakdown without losing a step.
 */
@Component
@ConditionalOnProperty(name = "ticketwave.saga.recovery-enabled", havingValue = "true", matchIfMissing = true)
public class SagaRecoveryJob {

    private final TicketOrderSagaOrchestrator orchestrator;

    public SagaRecoveryJob(TicketOrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${ticketwave.saga.recovery-interval-ms:30000}")
    public void recoverInterruptedSagas() {
        orchestrator.recover();
    }
}