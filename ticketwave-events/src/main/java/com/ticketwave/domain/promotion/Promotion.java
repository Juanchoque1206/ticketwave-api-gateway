package com.ticketwave.domain.promotion;
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
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promo_code", columnList = "code", unique = true),
        @Index(name = "idx_promo_venue", columnList = "venue_id")
})
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int maxUsage = 1;

    @Column(nullable = false)
    private int usedCount = 0;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}