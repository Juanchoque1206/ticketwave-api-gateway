package com.ticketwave.ticketorder.infrastructure.rest;

import com.ticketwave.ticketorder.application.port.UserData;
import com.ticketwave.ticketorder.application.port.UserGateway;
import com.ticketwave.ticketorder.infrastructure.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * REST adapter to the User aggregate exposed by the monolith.
 */
@Component
@Profile("!test")
public class RestUserGateway implements UserGateway {

    private final RestClient restClient;

    public RestUserGateway(RestClient monolithRestClient) {
        this.restClient = monolithRestClient;
    }

    @Override
    public UserData findByUsername(String username) {
        MonolithUserDto dto = restClient.get()
                .uri("/api/users/by-username/{username}", username)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        (request, response) -> {
                            throw new ResourceNotFoundException("User not found");
                        })
                .body(MonolithUserDto.class);
        return new UserData(dto.id(), dto.username());
    }
}
