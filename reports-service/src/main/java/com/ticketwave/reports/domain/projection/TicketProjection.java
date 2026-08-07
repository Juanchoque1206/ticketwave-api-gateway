package com.ticketwave.reports.domain.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection over the monolith {@code tickets} table (analytics read
 * replica). The event relation is mapped as a scalar id so the reporting
 * service stays decoupled from the monolith's own JPA graph.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
@Table(name = "tickets")
public class TicketProjection {

    @Id
    private UUID id;

    @Column(name = "qr_code", nullable = false, unique = true, length = 64)
    private String qrCode;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 50)
    private String seat;

    @Column(nullable = false, length = 20)
    private String status;

    private LocalDateTime issuedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime refundedAt;
}
