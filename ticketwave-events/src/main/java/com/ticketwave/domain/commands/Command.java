package com.ticketwave.domain.commands;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for every command sent by the saga orchestrator. Commands
 * request that an application use case performs a step of the workflow and are
 * routed over a dedicated command bus (RabbitMQ exchange in production).
 */
public interface Command {

    UUID commandId();

    Instant issuedAt();
}