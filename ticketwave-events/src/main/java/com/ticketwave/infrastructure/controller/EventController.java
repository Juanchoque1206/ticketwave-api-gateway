package com.ticketwave.infrastructure.controller;

import com.ticketwave.application.SearchEventsUseCase;
import com.ticketwave.application.EventService;
import com.ticketwave.infrastructure.dto.event.EventRequest;
import com.ticketwave.infrastructure.dto.event.EventResponse;
import com.ticketwave.infrastructure.dto.event.EventSearchRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "CRUD and search for events")
public class EventController {

    private final EventService eventService;
    private final SearchEventsUseCase searchEventsUseCase;

    public EventController(EventService eventService, SearchEventsUseCase searchEventsUseCase) {
        this.eventService = eventService;
        this.searchEventsUseCase = searchEventsUseCase;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) java.time.LocalDateTime fromDate,
            @RequestParam(required = false) java.time.LocalDateTime toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        EventSearchRequest filters = new EventSearchRequest(city, artist, venue, fromDate, toDate);
        return ResponseEntity.ok(searchEventsUseCase.search(filters, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> update(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        eventService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> reserveCapacity(@PathVariable UUID id, @RequestParam int quantity) {
        return ResponseEntity.ok(eventService.reserveCapacity(id, quantity));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> releaseCapacity(@PathVariable UUID id, @RequestParam int quantity) {
        eventService.releaseCapacity(id, quantity);
        return ResponseEntity.noContent().build();
    }
}