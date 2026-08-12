package com.ticketwave.ticketorder.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.ticketorder.infrastructure.bus.LocalCommandBus;
import com.ticketwave.ticketorder.infrastructure.bus.LocalEventBus;
import com.ticketwave.ticketorder.infrastructure.bus.RabbitMQCommandBusAdapter;
import com.ticketwave.ticketorder.infrastructure.bus.RabbitMQEventBusAdapter;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ is the shared cross-service bus. Each service manages its OWN queue
 * (configured via {@code ticketwave.bus.*}) bound to the shared topic exchanges,
 * so the order service receives the events/commands it owns without competing
 * with the legacy monolith or the orchestrator. The polymorphic type validator
 * only allows the shared contract packages, so a message published by the
 * monolith (e.g. PaymentAuthorized) is deserialized into the identical record
 * type this service keeps on its own classpath.
 */
@Configuration
public class EventBusConfig {

    @Bean
    @Profile("!rabbitmq & !test")
    public EventBus localEventBus() {
        return new LocalEventBus();
    }

    @Bean
    @Profile("!rabbitmq & !test")
    public CommandBus localCommandBus() {
        return new LocalCommandBus();
    }

    @Bean
    @Profile("rabbitmq")
    public EventBus rabbitMqEventBus(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin,
                                     @Value("${ticketwave.bus.events-queue:ticketwave.events.ticketorder}") String eventsQueue) {
        return new RabbitMQEventBusAdapter(rabbitTemplate, amqpAdmin, eventsQueue);
    }

    @Bean
    @Profile("rabbitmq")
    public CommandBus rabbitMqCommandBus(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin,
                                         @Value("${ticketwave.bus.commands-queue:ticketwave.commands.ticketorder}") String commandsQueue) {
        return new RabbitMQCommandBusAdapter(rabbitTemplate, amqpAdmin, commandsQueue);
    }

    @Bean
    @Profile("rabbitmq")
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(busObjectMapper());
    }

    private static ObjectMapper busObjectMapper() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ticketwave.domain.events.")
                .allowIfSubType("com.ticketwave.domain.commands.")
                .build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL, com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
