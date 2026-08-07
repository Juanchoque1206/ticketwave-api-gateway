package com.ticketwave.infrastructure.controller;

import com.ticketwave.domain.user.AppUser;
import com.ticketwave.infrastructure.dto.fraud.FraudGuardRequest;
import com.ticketwave.infrastructure.dto.fraud.MarkOrderRequest;
import com.ticketwave.infrastructure.dto.fraud.FraudReportResponse;
import com.ticketwave.application.FraudService;
import com.ticketwave.application.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@Tag(name = "Fraud", description = "Fraud pattern detection and duplicate prevention")
public class FraudController {

    private final FraudService fraudService;
    private final UserService userService;

    public FraudController(FraudService fraudService, UserService userService) {
        this.fraudService = fraudService;
        this.userService = userService;
    }

    @GetMapping("/check")
    public ResponseEntity<FraudReportResponse> check(Authentication authentication, HttpServletRequest request) {
        AppUser user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(fraudService.evaluate(user, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    @PostMapping("/guard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> guard(@RequestBody FraudGuardRequest request) {
        AppUser user = userService.getEntity(request.userId());
        fraudService.guard(user, request.ipAddress());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markOrder(@RequestBody MarkOrderRequest request) {
        AppUser user = userService.getEntity(request.userId());
        fraudService.markOrder(request.orderId(), user);
        return ResponseEntity.noContent().build();
    }
}