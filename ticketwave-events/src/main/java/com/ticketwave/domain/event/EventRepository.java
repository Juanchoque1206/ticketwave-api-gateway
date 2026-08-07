package com.ticketwave.domain.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    long count();

    Page<Event> findAll(Specification<Event> spec, Pageable pageable);

    Optional<Event> findById(UUID id);

    Event save(Event event);
}