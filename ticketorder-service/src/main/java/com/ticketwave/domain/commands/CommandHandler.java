package com.ticketwave.domain.commands;

/**
 * Functional handler contract used to register consumers of a command type.
 */
@FunctionalInterface
public interface CommandHandler<C extends Command> {

    void handle(C command);
}
