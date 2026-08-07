package com.ticketwave.domain.event;
import com.ticketwave.domain.venue.Venue;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_city", columnList = "city"),
        @Index(name = "idx_event_artist", columnList = "artist"),
        @Index(name = "idx_event_venue", columnList = "venue_id"),
        @Index(name = "idx_event_date", columnList = "eventDate")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String artist;

    @Column(length = 100)
    private String city;

    @Column(length = 200)
    private String venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venueEntity;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private int totalCapacity;

    @Column(nullable = false)
    private int reservedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    private LocalDateTime createdAt;
}