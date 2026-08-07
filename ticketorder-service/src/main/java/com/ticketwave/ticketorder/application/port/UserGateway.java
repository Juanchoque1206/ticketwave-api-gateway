package com.ticketwave.ticketorder.application.port;

import java.util.UUID;

/**
 * Outbound port to the User aggregate owned by the monolith. The TicketOrder
 * service only needs the immutable user identifier, resolved from the JWT
 * username claim on order creation.
 */
public interface UserGateway {

    UserData findByUsername(String username);
}
