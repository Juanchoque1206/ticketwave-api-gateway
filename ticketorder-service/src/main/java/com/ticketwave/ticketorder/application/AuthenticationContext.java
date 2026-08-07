package com.ticketwave.ticketorder.application;

/**
 * Authenticated caller context captured by the controller from the JWT
 * principal and the request metadata.
 */
public record AuthenticationContext(String username, String ipAddress) {
}
