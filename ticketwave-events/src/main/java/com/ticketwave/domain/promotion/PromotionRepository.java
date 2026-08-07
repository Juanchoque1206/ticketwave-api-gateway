package com.ticketwave.domain.promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    List<Promotion> findByActiveTrueAndValidUntilAfter(LocalDateTime now);

    List<Promotion> findByScope(PromotionScope scope);

    Promotion save(Promotion promotion);
}