package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.event.EventRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface JpaEventRepository extends EventRepository, JpaRepository<Event, UUID>,
        JpaSpecificationExecutor<Event> {
}