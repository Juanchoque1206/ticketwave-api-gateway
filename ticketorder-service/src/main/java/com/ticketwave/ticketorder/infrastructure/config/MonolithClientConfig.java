package com.ticketwave.ticketorder.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

/**
 * HTTP client used to reach the monolith for the synchronous operations the
 * TicketOrder service delegates to its aggregates (capacity, promotion, user,
 * fraud). Every call carries the internal service token so the monolith can
 * authenticate service-to-service requests without a user JWT.
 */
@Configuration
public class MonolithClientConfig {

    @Bean
    @Profile("!test")
    public RestClient monolithRestClient(@Value("${ticketwave.monolith.base-url}") String baseUrl,
                                         @Value("${ticketwave.monolith.internal-token}") String internalToken) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }
}
