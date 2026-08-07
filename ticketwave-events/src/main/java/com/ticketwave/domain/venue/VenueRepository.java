package com.ticketwave.domain.venue;

import java.util.Optional;
import java.util.UUID;

public interface VenueRepository {

    Optional<Venue> findByName(String name);

    Optional<Venue> findById(UUID id);

    Venue save(Venue venue);
}