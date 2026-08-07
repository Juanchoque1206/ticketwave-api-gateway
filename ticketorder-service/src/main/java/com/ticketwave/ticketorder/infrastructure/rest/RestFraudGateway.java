package com.ticketwave.ticketorder.infrastructure.rest;

import com.ticketwave.ticketorder.application.port.FraudGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * REST adapter to the Fraud aggregate exposed by the monolith.
 */
@Component
@Profile("!test")
public class RestFraudGateway implements FraudGateway {

    private final RestClient restClient;

    public RestFraudGateway(RestClient monolithRestClient) {
        this.restClient = monolithRestClient;
    }

    @Override
    public void guard(UUID userId, String ipAddress) {
        restClient.post()
                .uri("/api/fraud/guard")
                .body(new FraudGuardRequest(userId, ipAddress))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void markOrder(UUID orderId, UUID userId) {
        restClient.post()
                .uri("/api/fraud/orders")
                .body(new MarkOrderRequest(orderId, userId))
                .retrieve()
                .toBodilessEntity();
    }
}
