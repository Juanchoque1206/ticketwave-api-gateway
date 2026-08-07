package com.ticketwave.reports.domain.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationProjectionRepository extends JpaRepository<NotificationProjection, UUID> {

    List<NotificationProjection> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
