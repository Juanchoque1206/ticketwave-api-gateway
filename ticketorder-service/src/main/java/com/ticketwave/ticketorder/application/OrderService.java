package com.ticketwave.ticketorder.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.PromotionApplied;
import com.ticketwave.ticketorder.application.port.EventData;
import com.ticketwave.ticketorder.application.port.EventGateway;
import com.ticketwave.ticketorder.application.port.FraudGateway;
import com.ticketwave.ticketorder.application.port.PromotionGateway;
import com.ticketwave.ticketorder.application.port.PromotionQuote;
import com.ticketwave.ticketorder.application.port.UserData;
import com.ticketwave.ticketorder.application.port.UserGateway;
import com.ticketwave.ticketorder.domain.order.OrderStatus;
import com.ticketwave.ticketorder.domain.order.PriceCalculator;
import com.ticketwave.ticketorder.domain.order.TicketOrder;
import com.ticketwave.ticketorder.domain.order.TicketOrderRepository;
import com.ticketwave.ticketorder.infrastructure.dto.order.CreateOrderRequest;
import com.ticketwave.ticketorder.infrastructure.dto.order.OrderResponse;
import com.ticketwave.ticketorder.infrastructure.exception.OrderStateException;
import com.ticketwave.ticketorder.infrastructure.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order lifecycle service. Owns the TicketOrder aggregate and only talks to the
 * monolith (Event, Promotion, User, Fraud) through hexagonal gateway ports.
 */
@Service
public class OrderService {

    private final TicketOrderRepository orderRepository;
    private final EventGateway eventGateway;
    private final PromotionGateway promotionGateway;
    private final UserGateway userGateway;
    private final FraudGateway fraudGateway;
    private final EventBus eventBus;
    private final long orderTtlMinutes;

    public OrderService(TicketOrderRepository orderRepository,
                        EventGateway eventGateway,
                        PromotionGateway promotionGateway,
                        UserGateway userGateway,
                        FraudGateway fraudGateway,
                        EventBus eventBus,
                        @Value("${ticketwave.order-ttl-minutes:15}") long orderTtlMinutes) {
        this.orderRepository = orderRepository;
        this.eventGateway = eventGateway;
        this.promotionGateway = promotionGateway;
        this.userGateway = userGateway;
        this.fraudGateway = fraudGateway;
        this.eventBus = eventBus;
        this.orderTtlMinutes = orderTtlMinutes;
    }

    @Transactional
    public OrderResponse createReservation(AuthenticationContext ctx, CreateOrderRequest request) {
        UserData user = userGateway.findByUsername(ctx.username());
        fraudGateway.guard(user.id(), ctx.ipAddress());

        EventData event = eventGateway.getEvent(request.eventId());
        eventGateway.reserveCapacity(event.id(), request.quantity());

        TicketOrder order = new TicketOrder();
        order.setUserId(user.id());
        order.setEventId(event.id());
        order.setEventName(event.name());
        order.setQuantity(request.quantity());
        order.setReservedAt(LocalDateTime.now());
        order.setExpiresAt(LocalDateTime.now().plusMinutes(orderTtlMinutes));
        order.setStatus(OrderStatus.PENDING);

        BigDecimal subtotal = event.basePrice().multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal discount = BigDecimal.ZERO;
        String promotionCode = null;
        if (request.promotionCode() != null && !request.promotionCode().isBlank()) {
            PromotionQuote quote = promotionGateway.quote(request.promotionCode(), event.id(), request.quantity(), subtotal);
            discount = quote.discount();
            promotionCode = quote.code();
        }
        order.setTotalAmount(subtotal.subtract(discount));
        order.setDiscountAmount(discount);
        order.setPromotionCode(promotionCode);
        order = orderRepository.save(order);

        if (promotionCode != null) {
            promotionGateway.incrementUsage(promotionCode);
            eventBus.publish(new PromotionApplied(UUID.randomUUID(), Instant.now(),
                    order.getId(), promotionCode, discount));
        }

        fraudGateway.markOrder(order.getId(), user.id());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse confirm(UUID orderId) {
        TicketOrder order = requireOrder(orderId);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStateException("Only pending orders can be confirmed");
        }
        if (PriceCalculator.isExpired(order)) {
            throw new OrderStateException("Order has expired");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(UUID orderId) {
        TicketOrder order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStateException("Only pending orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        eventGateway.releaseCapacity(order.getEventId(), order.getQuantity());
        return toResponse(order);
    }

    @Transactional
    public void expire(UUID orderId) {
        TicketOrder order = requireOrder(orderId);
        if (order.getStatus() == OrderStatus.PENDING && PriceCalculator.isExpired(order)) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
            eventGateway.releaseCapacity(order.getEventId(), order.getQuantity());
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return toResponse(requireOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForUser(AuthenticationContext ctx) {
        UserData user = userGateway.findByUsername(ctx.username());
        return orderRepository.findByUserId(user.id()).stream().map(this::toResponse).toList();
    }

    private TicketOrder requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private OrderResponse toResponse(TicketOrder order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getEventId(), order.getEventName(),
                order.getStatus(), order.getQuantity(), order.getTotalAmount(), order.getDiscountAmount(),
                order.getReservedAt(), order.getExpiresAt());
    }
}
