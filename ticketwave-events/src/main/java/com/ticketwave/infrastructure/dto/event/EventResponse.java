package com.ticketwave.infrastructure.dto.event;

import com.ticketwave.domain.event.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String artist,
        String city,
        String venue,
        LocalDateTime eventDate,
        String description,
        BigDecimal basePrice,
        int totalCapacity,
        int availableCount,
        EventStatus status
) {
}