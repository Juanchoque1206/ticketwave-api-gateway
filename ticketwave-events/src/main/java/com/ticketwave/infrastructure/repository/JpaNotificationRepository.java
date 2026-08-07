package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.notification.Notification;
import com.ticketwave.domain.notification.NotificationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaNotificationRepository extends NotificationRepository, JpaRepository<Notification, UUID> {
}