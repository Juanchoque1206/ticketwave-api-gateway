package com.ticketwave.ticketorder.infrastructure.gateway;

import com.ticketwave.ticketorder.application.port.EventData;
import com.ticketwave.ticketorder.application.port.EventGateway;
import com.ticketwave.ticketorder.infrastructure.exception.BusinessRuleException;
import com.ticketwave.ticketorder.infrastructure.exception.ResourceNotFoundException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test double for the EventGateway. Seeds a mutable event read model and
 * simulates the monolith's capacity reservation rules in memory.
 */
public class FakeEventGateway implements EventGateway {

    private final Map<UUID, EventData> events = new ConcurrentHashMap<>();

    public void seed(EventData event) {
        events.put(event.id(), event);
    }

    @Override
    public EventData getEvent(UUID eventId) {
        EventData event = events.get(eventId);
        if (event == null) {
            throw new ResourceNotFoundException("Event not found");
        }
        return event;
    }

    @Override
    public int reserveCapacity(UUID eventId, int quantity) {
        EventData event = getEvent(eventId);
        if (event.availableCount() < quantity) {
            throw new BusinessRuleException("Not enough capacity available");
        }
        EventData next = new EventData(event.id(), event.name(), event.basePrice(),
                event.availableCount() - quantity, event.status());
        events.put(eventId, next);
        return next.availableCount();
    }

    @Override
    public void releaseCapacity(UUID eventId, int quantity) {
        EventData event = getEvent(eventId);
        EventData next = new EventData(event.id(), event.name(), event.basePrice(),
                event.availableCount() + quantity, event.status());
        events.put(eventId, next);
    }
}
