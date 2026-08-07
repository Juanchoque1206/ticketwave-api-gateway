package com.ticketwave.infrastructure.controller;

import com.ticketwave.infrastructure.dto.promotion.PromotionQuoteRequest;
import com.ticketwave.infrastructure.dto.promotion.PromotionQuoteResponse;
import com.ticketwave.infrastructure.dto.promotion.PromotionRequest;
import com.ticketwave.infrastructure.dto.promotion.PromotionResponse;
import com.ticketwave.application.EventService;
import com.ticketwave.application.PromotionService;
import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.promotion.Promotion;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@Tag(name = "Promotions", description = "National and venue-specific discount codes")
public class PromotionController {

    private final PromotionService promotionService;
    private final EventService eventService;

    public PromotionController(PromotionService promotionService, EventService eventService) {
        this.promotionService = promotionService;
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> listActive() {
        return ResponseEntity.ok(promotionService.listActive());
    }

    @PostMapping("/{code}/quote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionQuoteResponse> quote(@PathVariable String code,
                                                        @RequestBody PromotionQuoteRequest request) {
        Promotion promotion = promotionService.findByCode(code);
        Event event = eventService.getEntity(request.eventId());
        BigDecimal discount = promotionService.discountFor(promotion, event, request.quantity(), request.subtotal());
        return ResponseEntity.ok(new PromotionQuoteResponse(code.toUpperCase(), discount));
    }

    @PostMapping("/{code}/increment-usage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> incrementUsage(@PathVariable String code) {
        promotionService.incrementUsage(promotionService.findByCode(code));
        return ResponseEntity.noContent().build();
    }
}