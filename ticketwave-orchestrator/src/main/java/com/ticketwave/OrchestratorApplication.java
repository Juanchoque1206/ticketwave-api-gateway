package com.ticketwave;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Independent saga orchestrator service, extracted from ticketwave-events.
 * <p>
 * It owns the {@code ticketwave.events.orchestrator} queue and the
 * {@code ticketwave.commands} producer role: it consumes order/payment/ticket
 * domain events from RabbitMQ, advances the saga snapshot in Redis and drives
 * each workflow step by publishing commands that the other services handle.
 */
@SpringBootApplication
@EnableRabbit
@EnableScheduling
public class OrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}