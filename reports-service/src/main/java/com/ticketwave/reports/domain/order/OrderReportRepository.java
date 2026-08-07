package com.ticketwave.reports.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReportRepository extends JpaRepository<OrderReport, UUID> {

    Optional<OrderReport> findByOrderId(UUID orderId);

    List<OrderReport> findByReservedAtBetween(LocalDateTime from, LocalDateTime to);

    List<OrderReport> findByUserIdAndReservedAtBetween(UUID userId, LocalDateTime from, LocalDateTime to);
}
