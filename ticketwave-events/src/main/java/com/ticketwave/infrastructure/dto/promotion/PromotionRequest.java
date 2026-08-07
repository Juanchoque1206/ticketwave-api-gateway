package com.ticketwave.infrastructure.dto.promotion;

import com.ticketwave.domain.promotion.PromotionScope;
import com.ticketwave.domain.promotion.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromotionRequest(
        String code,
        String name,
        PromotionType type,
        BigDecimal value,
        PromotionScope scope,
        UUID venueId,
        int maxUsage,
        LocalDateTime validFrom,
        LocalDateTime validUntil
) {
}