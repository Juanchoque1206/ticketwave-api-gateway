package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.venue.Venue;
import com.ticketwave.domain.venue.VenueRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaVenueRepository extends VenueRepository, JpaRepository<Venue, UUID> {
}