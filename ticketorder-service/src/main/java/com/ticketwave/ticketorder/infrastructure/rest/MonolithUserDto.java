package com.ticketwave.ticketorder.infrastructure.rest;

import java.util.UUID;

/**
 * Subset of the monolith UserResponse consumed by the order service.
 */
public record MonolithUserDto(
        UUID id,
        String username,
        String email,
        String fullName,
        String city,
        String role) {
}
