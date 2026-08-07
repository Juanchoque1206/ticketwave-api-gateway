package com.ticketwave.reports.infrastructure.bus;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.bus.EventHandler;
import com.ticketwave.domain.events.DomainEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Production event transport backed by RabbitMQ. The reports service binds its
 * OWN queue ({@code ticketwave.events.reports}) to the shared
 * {@code ticketwave.events} topic exchange so every event published by the
 * platform is delivered to the reporting read model without competing with the
 * monolith and order consumers on {@code ticketwave.events.all}.
 */
public class RabbitMQEventBusAdapter implements EventBus {

    public static final String EXCHANGE = "ticketwave.events";
    public static final String QUEUE = "ticketwave.events.reports";
    public static final String ROUTING_KEY = "#";

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final Map<Class<?>, List<Consumer<DomainEvent>>> handlers = new ConcurrentHashMap<>();

    public RabbitMQEventBusAdapter(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @PostConstruct
    void declareTopology() {
        if (amqpAdmin == null) {
            return;
        }
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
    }

    @Override
    public void publish(DomainEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, event.getClass().getSimpleName(), event);
    }

    @RabbitListener(queues = QUEUE)
    public void onMessage(DomainEvent event) {
        for (Consumer<DomainEvent> consumer : handlers.getOrDefault(event.getClass(), List.of())) {
            consumer.accept(event);
        }
    }

    @Override
    public <E extends DomainEvent> void subscribe(Class<E> eventType, EventHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(event -> handler.handle(eventType.cast(event)));
    }
}
