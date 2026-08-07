package com.ticketwave.infrastructure.order;

import com.ticketwave.infrastructure.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * REST client to the ticketorder-service. Used by the legacy confirm endpoint to
 * resolve the order total (and user id) when the caller does not provide an
 * amount. The current user's bearer token is forwarded so the order service can
 * authenticate the lookup.
 */
@Component
public class OrderInfoClient {

    private final RestClient restClient;

    public OrderInfoClient(@Value("${ticketwave.order-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public OrderInfo getOrder(UUID orderId) {
        return restClient.get()
                .uri("/api/orders/{orderId}", orderId)
                .headers(this::forwardAuthorization)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new ResourceNotFoundException("Order not found");
                })
                .body(OrderInfo.class);
    }

    private void forwardAuthorization(HttpHeaders headers) {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest()
                : null;
    }
}
