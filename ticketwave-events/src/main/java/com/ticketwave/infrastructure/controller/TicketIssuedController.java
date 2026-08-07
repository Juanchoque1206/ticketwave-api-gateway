package com.ticketwave.infrastructure.controller;

import com.ticketwave.application.RefundTicketUseCase;
import com.ticketwave.application.TicketService;
import com.ticketwave.infrastructure.dto.ticket.TicketResponse;
import com.ticketwave.infrastructure.dto.ticket.ValidateTicketRequest;
import com.ticketwave.infrastructure.dto.ticket.ValidateTicketResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Management of issued digital tickets")
public class TicketIssuedController {

    private final TicketService ticketService;
    private final RefundTicketUseCase refundTicketUseCase;

    public TicketIssuedController(TicketService ticketService, RefundTicketUseCase refundTicketUseCase) {
        this.ticketService = ticketService;
        this.refundTicketUseCase = refundTicketUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.get(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TicketResponse>> listByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ticketService.listByOrder(orderId));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ValidateTicketResponse> validate(@RequestBody ValidateTicketRequest request) {
        return ResponseEntity.ok(ticketService.validate(request.qrCode()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TicketResponse> refund(@PathVariable UUID id) {
        return ResponseEntity.ok(refundTicketUseCase.refund(id));
    }
}