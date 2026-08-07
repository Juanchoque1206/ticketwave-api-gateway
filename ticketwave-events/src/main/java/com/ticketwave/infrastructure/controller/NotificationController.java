package com.ticketwave.infrastructure.controller;

import com.ticketwave.domain.user.AppUser;
import com.ticketwave.infrastructure.dto.notification.NotificationResponse;
import com.ticketwave.application.NotificationService;
import com.ticketwave.application.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Send and list notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> myNotifications(Authentication authentication) {
        AppUser user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(notificationService.listForUser(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }
}