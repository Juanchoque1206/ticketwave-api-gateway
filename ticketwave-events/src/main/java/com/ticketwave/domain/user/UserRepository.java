package com.ticketwave.domain.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<AppUser> findAll();

    Optional<AppUser> findById(UUID id);

    AppUser save(AppUser user);

    long count();
}