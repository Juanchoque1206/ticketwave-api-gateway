package com.ticketwave.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Jackson mapper used by infrastructure adapters (e.g. the saga state
 * snapshot in Redis). A plain mapper is enough: the AMQP message converter
 * builds its own typing-aware mapper in EventBusConfig.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}