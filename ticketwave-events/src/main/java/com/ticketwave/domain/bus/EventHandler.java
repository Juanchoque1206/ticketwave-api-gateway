package com.ticketwave.domain.bus;

import com.ticketwave.domain.events.DomainEvent;

/**
 * Functional handler contract used to register consumers of a domain event type.
 */
@FunctionalInterface
public interface EventHandler<E extends DomainEvent> {

    void handle(E event);
}