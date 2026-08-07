package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.user.AppUser;
import com.ticketwave.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserRepository extends UserRepository, JpaRepository<AppUser, UUID> {
}