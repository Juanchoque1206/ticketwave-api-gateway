package com.ticketwave.domain.bus;

import com.ticketwave.domain.events.DomainEvent;

@FunctionalInterface
public interface EventHandler<E extends DomainEvent> {

    void handle(E event);
}
