package com.ticketwave.reports.domain.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TicketProjectionRepository extends JpaRepository<TicketProjection, UUID> {

    List<TicketProjection> findByIssuedAtBetween(LocalDateTime from, LocalDateTime to);

    List<TicketProjection> findByStatusInAndIssuedAtBetween(List<String> statuses, LocalDateTime from, LocalDateTime to);

    List<TicketProjection> findByStatusIn(List<String> statuses);
}
