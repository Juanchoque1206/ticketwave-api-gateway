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
 * Read-only projection over the monolith {@code events} table (analytics read
 * replica). Immutable so the reporting service can never mutate business data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
@Table(name = "events")
public class EventProjection {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String artist;

    @Column(length = 100)
    private String city;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private int totalCapacity;

    @Column(nullable = false)
    private int reservedCount;

    @Column(nullable = false, length = 20)
    private String status;

    private LocalDateTime createdAt;
}
