package com.ticketwave.reports.config;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.reports.infrastructure.bus.InMemoryEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test infrastructure: an in-memory event bus double so the application context
 * loads offline without a broker. The real RabbitMQ transport is only active
 * under the rabbitmq profile.
 */
@Configuration
@Profile("test")
public class TestInfraConfig {

    @Bean
    public InMemoryEventBus inMemoryEventBus() {
        return new InMemoryEventBus();
    }
}
