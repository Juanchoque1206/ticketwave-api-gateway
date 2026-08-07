package com.ticketwave.infrastructure.dto;

import com.ticketwave.domain.user.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String city,
        Role role
) {
}