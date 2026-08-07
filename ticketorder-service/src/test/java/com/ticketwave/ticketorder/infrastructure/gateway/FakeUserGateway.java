package com.ticketwave.ticketorder.infrastructure.gateway;

import com.ticketwave.ticketorder.application.port.UserData;
import com.ticketwave.ticketorder.application.port.UserGateway;
import com.ticketwave.ticketorder.infrastructure.exception.ResourceNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test double for the UserGateway.
 */
public class FakeUserGateway implements UserGateway {

    private final Map<String, UserData> users = new ConcurrentHashMap<>();

    public void seed(UserData user) {
        users.put(user.username(), user);
    }

    @Override
    public UserData findByUsername(String username) {
        UserData user = users.get(username);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }
}
