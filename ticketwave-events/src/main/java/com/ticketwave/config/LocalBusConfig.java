package com.ticketwave.config;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.infrastructure.bus.InMemoryCommandBus;
import com.ticketwave.infrastructure.bus.InMemoryEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides in-memory bus implementations under the local profile, since the
 * production RabbitMQ adapters require a broker and are only active under the
 * rabbitmq profile.
 */
@Configuration
@Profile("local")
public class LocalBusConfig {

    @Bean
    public EventBus inMemoryEventBus() {
        return new InMemoryEventBus();
    }

    @Bean
    public CommandBus inMemoryCommandBus() {
        return new InMemoryCommandBus();
    }
}
