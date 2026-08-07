package com.ticketwave.application;

import com.ticketwave.domain.user.AppUser;
import com.ticketwave.domain.user.Role;
import com.ticketwave.infrastructure.dto.auth.AuthResponse;
import com.ticketwave.infrastructure.dto.auth.LoginRequest;
import com.ticketwave.infrastructure.dto.auth.RegisterRequest;
import com.ticketwave.infrastructure.exception.DuplicateResourceException;
import com.ticketwave.infrastructure.security.JwtService;
import com.ticketwave.domain.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setCity(request.city());
        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtService.generateToken(toUserDetails(user));
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getCity(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        AppUser user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        String token = jwtService.generateToken(userDetails);
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getCity(), user.getRole());
    }

    private UserDetails toUserDetails(AppUser user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}