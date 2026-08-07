package com.ticketwave.ticketorder;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity
@EnableRabbit
public class TicketOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketOrderApplication.class, args);
    }
}
