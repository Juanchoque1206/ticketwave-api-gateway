package com.ticketwave.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.event.EventStatus;
import com.ticketwave.domain.events.EventCancelled;
import com.ticketwave.domain.events.EventCreated;
import com.ticketwave.domain.events.EventUpdated;
import com.ticketwave.domain.venue.Venue;
import com.ticketwave.infrastructure.dto.event.EventRequest;
import com.ticketwave.infrastructure.dto.event.EventResponse;
import com.ticketwave.infrastructure.dto.event.EventSearchRequest;
import com.ticketwave.infrastructure.exception.BusinessRuleException;
import com.ticketwave.infrastructure.exception.ResourceNotFoundException;
import com.ticketwave.domain.event.EventRepository;
import com.ticketwave.domain.venue.VenueRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventBus eventBus;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository, EventBus eventBus) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.eventBus = eventBus;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> search(EventSearchRequest filters, Pageable pageable) {
        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filters.city() != null && !filters.city().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), filters.city().toLowerCase()));
            }
            if (filters.artist() != null && !filters.artist().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("artist")), "%" + filters.artist().toLowerCase() + "%"));
            }
            if (filters.venue() != null && !filters.venue().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("venue")), "%" + filters.venue().toLowerCase() + "%"));
            }
            if (filters.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), filters.fromDate()));
            }
            if (filters.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), filters.toDate()));
            }
            predicates.add(cb.notEqual(root.get("status"), EventStatus.CANCELLED));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return eventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse get(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        Event event = new Event();
        apply(event, request);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCreatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        eventBus.publish(new EventCreated(UUID.randomUUID(), Instant.now(),
                saved.getId(), saved.getName(), saved.getCity()));
        return toResponse(saved);
    }

    @Transactional
    public EventResponse update(UUID id, EventRequest request) {
        Event event = getEntity(id);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot update a cancelled event");
        }
        apply(event, request);
        Event saved = eventRepository.save(event);
        eventBus.publish(new EventUpdated(UUID.randomUUID(), Instant.now(), saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public void cancel(UUID id) {
        Event event = getEntity(id);
        event.setStatus(EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        eventBus.publish(new EventCancelled(UUID.randomUUID(), Instant.now(), saved.getId()));
    }

    @Transactional
    public EventResponse reserveCapacity(UUID id, int quantity) {
        Event event = getEntity(id);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("Event is cancelled");
        }
        int available = event.getTotalCapacity() - event.getReservedCount();
        if (quantity > available) {
            throw new BusinessRuleException("Not enough capacity available");
        }
        event.setReservedCount(event.getReservedCount() + quantity);
        if (event.getReservedCount() >= event.getTotalCapacity()) {
            event.setStatus(EventStatus.SOLD_OUT);
        }
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void releaseCapacity(UUID id, int quantity) {
        Event event = getEntity(id);
        event.setReservedCount(Math.max(0, event.getReservedCount() - quantity));
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
    }

    public Event getEntity(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private void apply(Event event, EventRequest request) {
        event.setName(request.name());
        event.setArtist(request.artist());
        event.setCity(request.city());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setDescription(request.description());
        event.setBasePrice(request.basePrice());
        event.setTotalCapacity(request.totalCapacity());
        if (request.venue() != null) {
            venueRepository.findByName(request.venue()).ifPresent(event::setVenueEntity);
        }
    }

    private EventResponse toResponse(Event event) {
        int available = event.getTotalCapacity() - event.getReservedCount();
        return new EventResponse(event.getId(), event.getName(), event.getArtist(), event.getCity(),
                event.getVenue(), event.getEventDate(), event.getDescription(), event.getBasePrice(),
                event.getTotalCapacity(), available, event.getStatus());
    }
}