package com.ticketwave.reports.domain.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventProjectionRepository extends JpaRepository<EventProjection, UUID> {
}
