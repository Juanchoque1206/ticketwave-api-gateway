package com.ticketwave.ticketorder.infrastructure.rest;

import com.ticketwave.ticketorder.application.port.PromotionGateway;
import com.ticketwave.ticketorder.application.port.PromotionQuote;
import com.ticketwave.ticketorder.infrastructure.exception.BusinessRuleException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST adapter to the Promotion aggregate exposed by the monolith.
 */
@Component
@Profile("!test")
public class RestPromotionGateway implements PromotionGateway {

    private final RestClient restClient;

    public RestPromotionGateway(RestClient monolithRestClient) {
        this.restClient = monolithRestClient;
    }

    @Override
    public PromotionQuote quote(String code, UUID eventId, int quantity, BigDecimal subtotal) {
        PromotionQuoteResponse response = restClient.post()
                .uri("/api/promotions/{code}/quote", code)
                .body(new PromotionQuoteRequest(eventId, quantity, subtotal))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, res) -> {
                    throw new BusinessRuleException("Promotion rejected: " + res.getStatusText());
                })
                .body(PromotionQuoteResponse.class);
        return new PromotionQuote(response.code(), response.discount());
    }

    @Override
    public void incrementUsage(String code) {
        restClient.post()
                .uri("/api/promotions/{code}/increment-usage", code)
                .retrieve()
                .toBodilessEntity();
    }
}
