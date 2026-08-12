package com.ticketwave.infrastructure.bus;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.commands.Command;
import com.ticketwave.domain.commands.CommandHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Orchestrator command transport backed by RabbitMQ. The orchestrator only
 * PUBLISHES commands (it never executes them), so this adapter declares the
 * shared {@code ticketwave.commands} topic exchange and sends each command using
 * its simple class name as the routing key. Handlers registered via
 * {@link #subscribe} are kept in-memory (used by tests / local profiles only).
 */
public class RabbitMQCommandBusAdapter implements CommandBus {

    public static final String EXCHANGE = "ticketwave.commands";

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final Map<Class<?>, List<Consumer<Command>>> handlers = new ConcurrentHashMap<>();

    public RabbitMQCommandBusAdapter(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @PostConstruct
    void declareTopology() {
        if (amqpAdmin == null) {
            return;
        }
        amqpAdmin.declareExchange(new TopicExchange(EXCHANGE, true, false));
    }

    @Override
    public void send(Command command) {
        rabbitTemplate.convertAndSend(EXCHANGE, command.getClass().getSimpleName(), command);
    }

    @Override
    public <C extends Command> void subscribe(Class<C> commandType, CommandHandler<C> handler) {
        handlers.computeIfAbsent(commandType, k -> new CopyOnWriteArrayList<>())
                .add(command -> handler.handle(commandType.cast(command)));
    }
}