package com.ticketwave.infrastructure.venue;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.EventCancelled;
import com.ticketwave.domain.events.EventCreated;
import com.ticketwave.domain.events.EventUpdated;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscriber adapter that keeps the external venue management system in sync
 * with event and order lifecycle domain events.
 */
@Component
public class VenueEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(VenueEventSubscriber.class);

    public VenueEventSubscriber(EventBus eventBus) {
        eventBus.subscribe(EventCreated.class, this::onEventCreated);
        eventBus.subscribe(EventUpdated.class, this::onEventUpdated);
        eventBus.subscribe(EventCancelled.class, this::onEventCancelled);
        eventBus.subscribe(TicketOrderCreated.class, this::onCreated);
        eventBus.subscribe(TicketOrderCancelled.class, this::onCancelled);
    }

    private void onEventCreated(EventCreated event) {
        log.info("Venue system: register event {} '{}' in {}", event.eventId(), event.name(), event.city());
    }

    private void onEventUpdated(EventUpdated event) {
        log.info("Venue system: refresh event {}", event.eventId());
    }

    private void onEventCancelled(EventCancelled event) {
        log.info("Venue system: release venue for event {}", event.eventId());
    }

    private void onCreated(TicketOrderCreated event) {
        log.info("Venue system: hold {} seats for event {}", event.quantity(), event.eventId());
    }

    private void onCancelled(TicketOrderCancelled event) {
        log.info("Venue system: release {} seats for event {}", event.quantity(), event.eventId());
    }
}