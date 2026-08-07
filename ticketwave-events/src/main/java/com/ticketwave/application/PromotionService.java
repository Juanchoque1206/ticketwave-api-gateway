package com.ticketwave.application;

import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.promotion.Promotion;
import com.ticketwave.domain.promotion.PromotionScope;
import com.ticketwave.domain.venue.Venue;
import com.ticketwave.infrastructure.dto.promotion.PromotionRequest;
import com.ticketwave.infrastructure.dto.promotion.PromotionResponse;
import com.ticketwave.infrastructure.exception.BusinessRuleException;
import com.ticketwave.infrastructure.exception.ResourceNotFoundException;
import com.ticketwave.domain.promotion.PromotionRepository;
import com.ticketwave.domain.venue.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final VenueRepository venueRepository;

    public PromotionService(PromotionRepository promotionRepository, VenueRepository venueRepository) {
        this.promotionRepository = promotionRepository;
        this.venueRepository = venueRepository;
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        promotionRepository.findByCodeIgnoreCase(request.code())
                .ifPresent(p -> {
                    throw new BusinessRuleException("Promotion code already exists");
                });

        Promotion promotion = new Promotion();
        promotion.setCode(request.code().toUpperCase());
        promotion.setName(request.name());
        promotion.setType(request.type());
        promotion.setValue(request.value());
        promotion.setScope(request.scope());
        promotion.setMaxUsage(request.maxUsage());
        promotion.setValidFrom(request.validFrom());
        promotion.setValidUntil(request.validUntil());
        if (request.scope() == PromotionScope.VENUE_SPECIFIC && request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
            promotion.setVenue(venue);
        }
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional(readOnly = true)
    public Promotion findByCode(String code) {
        return promotionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
    }

    @Transactional
    public void incrementUsage(Promotion promotion) {
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepository.save(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listActive() {
        return promotionRepository.findByActiveTrueAndValidUntilAfter(LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    public BigDecimal discountFor(Promotion promotion, Event event, int quantity, BigDecimal subtotal) {
        if (!promotion.isActive() || promotion.getUsedCount() >= promotion.getMaxUsage()) {
            throw new BusinessRuleException("Promotion is no longer valid");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean notYetValid = promotion.getValidFrom() != null && now.isBefore(promotion.getValidFrom());
        boolean expired = promotion.getValidUntil() != null && now.isAfter(promotion.getValidUntil());
        if (notYetValid || expired) {
            throw new BusinessRuleException("Promotion is not valid at this time");
        }
        if (promotion.getScope() == PromotionScope.VENUE_SPECIFIC
                && promotion.getVenue() != null
                && !promotion.getVenue().getName().equalsIgnoreCase(event.getVenue())) {
            throw new BusinessRuleException("Promotion does not apply to this event venue");
        }
        return switch (promotion.getType()) {
            case PERCENTAGE -> subtotal.multiply(promotion.getValue()).divide(BigDecimal.valueOf(100));
            case FIXED_AMOUNT -> promotion.getValue().multiply(BigDecimal.valueOf(quantity));
        };
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getCode(), promotion.getName(),
                promotion.getType(), promotion.getValue(), promotion.getScope(),
                promotion.getVenue() != null ? promotion.getVenue().getId() : null,
                promotion.isActive(), promotion.getMaxUsage(), promotion.getUsedCount(),
                promotion.getValidFrom(), promotion.getValidUntil());
    }
}