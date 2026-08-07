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

import java.util.UUID;

/**
 * Read-only projection over the monolith {@code app_users} table (analytics read
 * replica). Used to decorate ticket and notification reports with usernames.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
@Table(name = "app_users")
public class AppUserProjection {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 120)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private boolean enabled = true;
}
