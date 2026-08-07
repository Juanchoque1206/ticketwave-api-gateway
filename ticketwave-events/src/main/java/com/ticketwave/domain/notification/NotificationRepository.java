package com.ticketwave.domain.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Notification> findById(UUID id);
}