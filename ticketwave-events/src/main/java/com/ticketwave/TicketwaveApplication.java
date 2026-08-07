package com.ticketwave;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableMethodSecurity
@EnableRabbit
public class TicketwaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketwaveApplication.class, args);
    }
}