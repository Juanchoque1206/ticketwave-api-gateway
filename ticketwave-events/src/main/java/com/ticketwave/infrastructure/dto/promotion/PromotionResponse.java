package com.ticketwave.infrastructure.dto.promotion;

import com.ticketwave.domain.promotion.PromotionScope;
import com.ticketwave.domain.promotion.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        String code,
        String name,
        PromotionType type,
        BigDecimal value,
        PromotionScope scope,
        UUID venueId,
        boolean active,
        int maxUsage,
        int usedCount,
        LocalDateTime validFrom,
        LocalDateTime validUntil
) {
}