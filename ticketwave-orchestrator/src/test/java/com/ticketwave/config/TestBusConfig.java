package com.ticketwave.config;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.infrastructure.bus.InMemoryCommandBus;
import com.ticketwave.infrastructure.bus.InMemoryEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides in-memory bus doubles under the test profile, since the production
 * RabbitMQ adapters require a broker. Production code never sees these.
 */
@Configuration
@Profile("test")
public class TestBusConfig {

    @Bean
    public EventBus inMemoryEventBus() {
        return new InMemoryEventBus();
    }

    @Bean
    public CommandBus inMemoryCommandBus() {
        return new InMemoryCommandBus();
    }
}