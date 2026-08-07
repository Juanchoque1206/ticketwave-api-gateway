package com.ticketwave.ticketorder.infrastructure.exception;

public class OrderStateException extends RuntimeException {

    public OrderStateException(String message) {
        super(message);
    }
}
