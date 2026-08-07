package com.ticketwave.domain.bus;

import com.ticketwave.domain.commands.Command;
import com.ticketwave.domain.commands.CommandHandler;

/**
 * Hexagonal port for command delivery used by the saga orchestrator. Commands
 * request an action from a bounded service (payment, ticket issuance, refund);
 * the concrete transport (in-memory, RabbitMQ) is chosen in infrastructure.
 */
public interface CommandBus {

    void send(Command command);

    <C extends Command> void subscribe(Class<C> commandType, CommandHandler<C> handler);
}
