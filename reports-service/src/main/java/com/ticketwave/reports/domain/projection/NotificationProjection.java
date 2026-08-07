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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection over the monolith {@code notifications} table (analytics
 * read replica). The user relation is mapped as a scalar id.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
@Table(name = "notifications")
public class NotificationProjection {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false)
    private boolean read = false;

    private LocalDateTime createdAt;
}
