package com.ticketwave.reports.domain.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentProjectionRepository extends JpaRepository<PaymentProjection, UUID> {

    List<PaymentProjection> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    List<PaymentProjection> findByStatusInAndPaidAtBetween(List<String> statuses, LocalDateTime from, LocalDateTime to);

    List<PaymentProjection> findByStatusIn(List<String> statuses);
}
