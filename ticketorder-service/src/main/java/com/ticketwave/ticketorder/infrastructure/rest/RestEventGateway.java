package com.ticketwave.ticketorder.infrastructure.rest;

import com.ticketwave.ticketorder.application.port.EventData;
import com.ticketwave.ticketorder.application.port.EventGateway;
import com.ticketwave.ticketorder.infrastructure.exception.BusinessRuleException;
import com.ticketwave.ticketorder.infrastructure.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * REST adapter to the Event aggregate exposed by the monolith.
 */
@Component
@Profile("!test")
public class RestEventGateway implements EventGateway {

    private final RestClient restClient;

    public RestEventGateway(RestClient monolithRestClient) {
        this.restClient = monolithRestClient;
    }

    @Override
    public EventData getEvent(UUID eventId) {
        MonolithEventDto dto = restClient.get()
                .uri("/api/events/{eventId}", eventId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, response) -> {
                            throw new ResourceNotFoundException("Event not found");
                        })
                .body(MonolithEventDto.class);
        return toData(dto);
    }

    @Override
    public int reserveCapacity(UUID eventId, int quantity) {
        MonolithEventDto dto = restClient.post()
                .uri("/api/events/{eventId}/reserve?quantity={quantity}", eventId, quantity)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new BusinessRuleException(parseMessage(response));
                })
                .body(MonolithEventDto.class);
        return dto.availableCount();
    }

    @Override
    public void releaseCapacity(UUID eventId, int quantity) {
        restClient.post()
                .uri("/api/events/{eventId}/release?quantity={quantity}", eventId, quantity)
                .retrieve()
                .toBodilessEntity();
    }

    private EventData toData(MonolithEventDto dto) {
        return new EventData(dto.id(), dto.name(), dto.basePrice(), dto.availableCount(), dto.status());
    }

    private String parseMessage(org.springframework.http.client.ClientHttpResponse response) {
        try {
            if (response.getBody() != null) {
                byte[] bytes = response.getBody().readAllBytes();
                String body = new String(bytes);
                int idx = body.indexOf("\"message\":\"");
                if (idx >= 0) {
                    int start = idx + "\"message\":\"".length();
                    int end = body.indexOf("\"", start);
                    if (end > start) {
                        return body.substring(start, end);
                    }
                }
                return "Monolith rejected the request";
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "Monolith rejected the request";
    }
}
