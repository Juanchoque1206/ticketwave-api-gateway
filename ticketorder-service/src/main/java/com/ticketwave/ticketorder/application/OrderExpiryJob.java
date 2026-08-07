package com.ticketwave.ticketorder.application;

import com.ticketwave.ticketorder.domain.order.OrderStatus;
import com.ticketwave.ticketorder.domain.order.TicketOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Expires pending orders that outlived their reservation window, releasing the
 * capacity they held in the Event aggregate.
 */
@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final TicketOrderRepository orderRepository;
    private final OrderService orderService;

    public OrderExpiryJob(TicketOrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(cron = "${ticketwave.order-expiry-cron:*/30 * * * * *}")
    public void expirePendingOrders() {
        orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .filter(o -> o.getExpiresAt() != null && o.getExpiresAt().isBefore(LocalDateTime.now()))
                .forEach(o -> {
                    orderService.expire(o.getId());
                    log.info("Expired order {}", o.getId());
                });
    }
}
