package com.ticketwave.ticketorder.application.port;

import java.util.UUID;

/**
 * Minimal read model of a User as seen from the TicketOrder service.
 */
public record UserData(
        UUID id,
        String username) {
}
