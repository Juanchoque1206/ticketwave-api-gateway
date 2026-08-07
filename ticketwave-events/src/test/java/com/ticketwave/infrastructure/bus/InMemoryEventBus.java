package com.ticketwave.infrastructure.bus;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.DomainEvent;
import com.ticketwave.domain.bus.EventHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Test-only in-memory EventBus double, active under the test profile so the
 * application context loads without a RabbitMQ broker.
 */
public class InMemoryEventBus implements EventBus {

    private final Map<Class<?>, List<Consumer<DomainEvent>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
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