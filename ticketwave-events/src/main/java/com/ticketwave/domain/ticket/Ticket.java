package com.ticketwave.domain.ticket;
import com.ticketwave.domain.event.Event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_code", columnList = "qrCode", unique = true),
        @Index(name = "idx_ticket_order", columnList = "order_id")
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String qrCode;

    /**
     * Scalar reference to the TicketOrder aggregate, which now lives in the
     * ticketorder-service. No JPA relationship is held across the boundary.
     */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price;

    @Column(length = 50)
    private String seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.EMITTED;

    private LocalDateTime issuedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime refundedAt;
}
