package com.ticketwave.infrastructure.bus;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.commands.Command;
import com.ticketwave.domain.commands.CommandHandler;
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
 * Production command transport. Commands are routed through a dedicated
 * {@code ticketwave.commands} topic exchange (separate from the event exchange)
 * and consumed through a bound queue so every instance of the monolith executes
 * the requested workflow step. Used when the rabbitmq profile is active.
 */
public class RabbitMQCommandBusAdapter implements CommandBus {

    public static final String EXCHANGE = "ticketwave.commands";
    public static final String QUEUE = "ticketwave.commands.all";
    public static final String ROUTING_KEY = "#";

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
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
    }

    @Override
    public void send(Command command) {
        rabbitTemplate.convertAndSend(EXCHANGE, command.getClass().getSimpleName(), command);
    }

    @RabbitListener(queues = QUEUE)
    public void onMessage(Command command) {
        for (Consumer<Command> consumer : handlers.getOrDefault(command.getClass(), List.of())) {
            consumer.accept(command);
        }
    }

    @Override
    public <C extends Command> void subscribe(Class<C> commandType, CommandHandler<C> handler) {
        handlers.computeIfAbsent(commandType, k -> new CopyOnWriteArrayList<>())
                .add(command -> handler.handle(commandType.cast(command)));
    }
}