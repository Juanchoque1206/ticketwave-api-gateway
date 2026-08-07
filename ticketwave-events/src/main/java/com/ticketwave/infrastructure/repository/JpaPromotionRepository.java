package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.promotion.Promotion;
import com.ticketwave.domain.promotion.PromotionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPromotionRepository extends PromotionRepository, JpaRepository<Promotion, UUID> {
}