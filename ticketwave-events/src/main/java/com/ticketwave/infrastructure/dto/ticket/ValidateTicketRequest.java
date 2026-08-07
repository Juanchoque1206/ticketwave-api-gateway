package com.ticketwave.infrastructure.dto.ticket;

import jakarta.validation.constraints.NotBlank;

public record ValidateTicketRequest(
        @NotBlank String qrCode
) {
}