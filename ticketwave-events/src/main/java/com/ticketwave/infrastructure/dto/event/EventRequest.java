package com.ticketwave.infrastructure.dto.event;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank String name,
        String artist,
        String city,
        String venue,
        @NotNull LocalDateTime eventDate,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal basePrice,
        @NotNull @Positive Integer totalCapacity
) {
}