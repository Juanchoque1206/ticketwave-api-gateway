package com.ticketwave.application;

import com.ticketwave.infrastructure.dto.event.EventResponse;
import com.ticketwave.infrastructure.dto.event.EventSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchEventsUseCase {

    private final EventService eventService;

    public SearchEventsUseCase(EventService eventService) {
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> search(EventSearchRequest filters, Pageable pageable) {
        return eventService.search(filters, pageable);
    }
}