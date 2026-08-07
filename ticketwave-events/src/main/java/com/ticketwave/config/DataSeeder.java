package com.ticketwave.config;

import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.event.EventStatus;
import com.ticketwave.domain.notification.Notification;
import com.ticketwave.domain.notification.NotificationChannel;
import com.ticketwave.domain.notification.NotificationType;
import com.ticketwave.domain.payment.Payment;
import com.ticketwave.domain.payment.PaymentProvider;
import com.ticketwave.domain.payment.PaymentStatus;
import com.ticketwave.domain.promotion.Promotion;
import com.ticketwave.domain.promotion.PromotionScope;
import com.ticketwave.domain.promotion.PromotionType;
import com.ticketwave.domain.ticket.Ticket;
import com.ticketwave.domain.ticket.TicketStatus;
import com.ticketwave.domain.user.AppUser;
import com.ticketwave.domain.user.Role;
import com.ticketwave.domain.venue.Venue;
import com.ticketwave.domain.event.EventRepository;
import com.ticketwave.domain.notification.NotificationRepository;
import com.ticketwave.domain.payment.PaymentRepository;
import com.ticketwave.domain.promotion.PromotionRepository;
import com.ticketwave.domain.ticket.TicketRepository;
import com.ticketwave.domain.user.UserRepository;
import com.ticketwave.domain.venue.VenueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@Profile("local")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seed(UserRepository userRepository,
                                  EventRepository eventRepository,
                                  VenueRepository venueRepository,
                                  PromotionRepository promotionRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setEmail("admin@ticketwave.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setFullName("TicketWave Admin");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            AppUser user = new AppUser();
            user.setUsername("user");
            user.setEmail("user@ticketwave.com");
            user.setPassword(passwordEncoder.encode("user1234"));
            user.setFullName("Regular User");
            user.setCity("Lima");
            user.setRole(Role.USER);
            userRepository.save(user);

            Venue arena = new Venue();
            arena.setName("National Stadium");
            arena.setCity("Lima");
            arena.setAddress("Av. San Luis");
            arena.setCapacity(50000);
            venueRepository.save(arena);

            Event event = new Event();
            event.setName("Summer Music Festival");
            event.setArtist("Various Artists");
            event.setCity("Lima");
            event.setVenue("National Stadium");
            event.setVenueEntity(arena);
            event.setEventDate(LocalDateTime.now().plusDays(30));
            event.setDescription("Annual summer festival");
            event.setBasePrice(new BigDecimal("150.00"));
            event.setTotalCapacity(500);
            event.setReservedCount(0);
            event.setStatus(EventStatus.PUBLISHED);
            event.setCreatedAt(LocalDateTime.now());
            eventRepository.save(event);

            Promotion promo = new Promotion();
            promo.setCode("WELCOME10");
            promo.setName("Welcome discount");
            promo.setType(PromotionType.PERCENTAGE);
            promo.setValue(new BigDecimal("10.00"));
            promo.setScope(PromotionScope.NATIONAL);
            promo.setMaxUsage(1000);
            promo.setUsedCount(0);
            promo.setValidFrom(LocalDateTime.now().minusDays(1));
            promo.setValidUntil(LocalDateTime.now().plusDays(90));
            promo.setActive(true);
            promotionRepository.save(promo);

            log.info("Seed data created. admin/admin1234, user/user1234");
        };
    }
}