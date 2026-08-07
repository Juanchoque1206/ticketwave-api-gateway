package com.ticketwave.infrastructure.dto.notification;

import com.ticketwave.domain.notification.NotificationChannel;
import com.ticketwave.domain.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        NotificationChannel channel,
        String subject,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
}