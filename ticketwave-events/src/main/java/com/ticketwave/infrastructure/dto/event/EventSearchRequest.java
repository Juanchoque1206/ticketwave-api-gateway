package com.ticketwave.infrastructure.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Search filters for events")
public record EventSearchRequest(
        String city,
        String artist,
        String venue,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {
}