package com.ticketwave.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Centralized RabbitMQ topology managed by the orchestrator. Since the
 * orchestrator is the central coordinator of the saga workflow, it declares the
 * complete messaging topology for the platform: shared exchanges, per-service
 * queues, and their bindings.
 *
 * <h3>Exchanges</h3>
 * <ul>
 *   <li>{@code ticketwave.events} &ndash; topic exchange for domain events</li>
 *   <li>{@code ticketwave.commands} &ndash; topic exchange for saga commands</li>
 * </ul>
 *
 * <h3>Queues and bindings</h3>
 * <table>
 *   <tr><th>Service</th><th>Events queue</th><th>Commands queue</th></tr>
 *   <tr><td>orchestrator</td><td>ticketwave.events.orchestrator</td><td>&mdash;</td></tr>
 *   <tr><td>events-service</td><td>ticketwave.events.events-service</td><td>ticketwave.commands.events-service</td></tr>
 *   <tr><td>ticketorder</td><td>ticketwave.events.ticketorder</td><td>ticketwave.commands.ticketorder</td></tr>
 * </table>
 *
 * Each queue binds to its exchange with routing key {@code #} so every message
 * is delivered to every service independently (no competing consumers).
 */
@Configuration
@Profile("rabbitmq")
public class RabbitMQTopologyConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQTopologyConfig.class);

    private static final String EVENTS_EXCHANGE = "ticketwave.events";
    private static final String COMMANDS_EXCHANGE = "ticketwave.commands";
    private static final String ROUTING_KEY = "#";

    // -- Per-service queue names --------------------------------------------------

    private static final String EVENTS_QUEUE_ORCHESTRATOR = "ticketwave.events.orchestrator";

    private static final String EVENTS_QUEUE_EVENTS_SERVICE = "ticketwave.events.events-service";
    private static final String COMMANDS_QUEUE_EVENTS_SERVICE = "ticketwave.commands.events-service";

    private static final String EVENTS_QUEUE_TICKETORDER = "ticketwave.events.ticketorder";
    private static final String COMMANDS_QUEUE_TICKETORDER = "ticketwave.commands.ticketorder";

    private final AmqpAdmin amqpAdmin;

    public RabbitMQTopologyConfig(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @PostConstruct
    void declareFullTopology() {
        log.info("Declaring centralized RabbitMQ topology");

        // ---- Exchanges ----------------------------------------------------------
        TopicExchange eventsExchange = new TopicExchange(EVENTS_EXCHANGE, true, false);
        TopicExchange commandsExchange = new TopicExchange(COMMANDS_EXCHANGE, true, false);

        amqpAdmin.declareExchange(eventsExchange);
        amqpAdmin.declareExchange(commandsExchange);

        // ---- Orchestrator queues ------------------------------------------------
        declareAndBind(EVENTS_QUEUE_ORCHESTRATOR, eventsExchange);

        // ---- Events-service queues ----------------------------------------------
        declareAndBind(EVENTS_QUEUE_EVENTS_SERVICE, eventsExchange);
        declareAndBind(COMMANDS_QUEUE_EVENTS_SERVICE, commandsExchange);

        // ---- Ticketorder-service queues -----------------------------------------
        declareAndBind(EVENTS_QUEUE_TICKETORDER, eventsExchange);
        declareAndBind(COMMANDS_QUEUE_TICKETORDER, commandsExchange);

        log.info("RabbitMQ topology declared: 2 exchanges, 5 queues");
    }

    private void declareAndBind(String queueName, TopicExchange exchange) {
        Queue queue = new Queue(queueName, true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);
        log.debug("Declared queue [{}] bound to exchange [{}]", queueName, exchange.getName());
    }
}
