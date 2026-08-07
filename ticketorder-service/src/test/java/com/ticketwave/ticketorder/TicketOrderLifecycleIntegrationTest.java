package com.ticketwave.ticketorder;

import com.ticketwave.domain.commands.CancelTicketOrderCommand;
import com.ticketwave.domain.events.PaymentAuthorized;
import com.ticketwave.domain.events.PromotionApplied;
import com.ticketwave.domain.events.TicketOrderCancelled;
import com.ticketwave.domain.events.TicketOrderConfirmed;
import com.ticketwave.domain.events.TicketOrderCreated;
import com.ticketwave.ticketorder.application.AuthenticationContext;
import com.ticketwave.ticketorder.application.CreateOrderUseCase;
import com.ticketwave.ticketorder.application.OrderService;
import com.ticketwave.ticketorder.application.port.EventData;
import com.ticketwave.ticketorder.application.port.UserData;
import com.ticketwave.ticketorder.domain.order.OrderStatus;
import com.ticketwave.ticketorder.domain.order.TicketOrderRepository;
import com.ticketwave.ticketorder.infrastructure.bus.InMemoryCommandBus;
import com.ticketwave.ticketorder.infrastructure.bus.InMemoryEventBus;
import com.ticketwave.ticketorder.infrastructure.dto.order.CreateOrderRequest;
import com.ticketwave.ticketorder.infrastructure.dto.order.OrderResponse;
import com.ticketwave.ticketorder.infrastructure.gateway.FakeEventGateway;
import com.ticketwave.ticketorder.infrastructure.gateway.FakeFraudGateway;
import com.ticketwave.ticketorder.infrastructure.gateway.FakePromotionGateway;
import com.ticketwave.ticketorder.infrastructure.gateway.FakeUserGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TicketOrderLifecycleIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();

    @Autowired
    private OrderService orderService;
    @Autowired
    private CreateOrderUseCase createOrderUseCase;
    @Autowired
    private TicketOrderRepository orderRepository;
    @Autowired
    private FakeEventGateway eventGateway;
    @Autowired
    private FakeUserGateway userGateway;
    @Autowired
    private FakePromotionGateway promotionGateway;
    @Autowired
    private FakeFraudGateway fraudGateway;
    @Autowired
    private InMemoryEventBus eventBus;
    @Autowired
    private InMemoryCommandBus commandBus;

    @BeforeEach
    void seedExternalAggregates() {
        eventGateway.seed(new EventData(EVENT_ID, "Summer Music Festival",
                new BigDecimal("100.00"), 100, "PUBLISHED"));
        userGateway.seed(new UserData(USER_ID, "buyer"));
        promotionGateway.seed("WELCOME10", new BigDecimal("10.00"));
        fraudGateway.reset();
        promotionGateway.reset();
        eventBus.clear();
    }

    @Test
    void createOrder_persistsPendingOrder_reservesCapacity_publishesCreated() {
        OrderResponse order = reserve(2, null);

        assertNotNull(order.id());
        assertEquals(USER_ID, order.userId());
        assertEquals(EVENT_ID, order.eventId());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(new BigDecimal("200.00"), order.totalAmount());
        assertEquals(BigDecimal.ZERO, order.discountAmount());
        assertNotNull(order.expiresAt());

        assertEquals(98, eventGateway.getEvent(EVENT_ID).availableCount());
        assertEquals(1, fraudGateway.guardCount());
        assertEquals(1, fraudGateway.markedOrderCount());

        assertFalse(eventBus.published(TicketOrderCreated.class).isEmpty());
    }

    @Test
    void createOrder_withPromotion_appliesDiscountAndIncrementsUsage() {
        OrderResponse order = reserve(2, "WELCOME10");

        assertEquals(new BigDecimal("190.00"), order.totalAmount());
        assertEquals(new BigDecimal("10.00"), order.discountAmount());
        assertEquals(1, promotionGateway.usageIncrements());
        assertFalse(eventBus.published(PromotionApplied.class).isEmpty());
    }

    @Test
    void paymentAuthorized_confirmsOrder_publishesConfirmed() {
        OrderResponse order = reserve(2, null);

        eventBus.publish(new PaymentAuthorized(UUID.randomUUID(), Instant.now(),
                order.id(), order.userId(), order.totalAmount(), "TXN-1"));

        OrderResponse confirmed = orderService.getOrder(order.id());
        assertEquals(OrderStatus.CONFIRMED, confirmed.status());
        assertFalse(eventBus.published(TicketOrderConfirmed.class).isEmpty());
    }

    @Test
    void cancelCommand_cancelsOrder_releasesCapacity_publishesCancelled() {
        OrderResponse order = reserve(2, null);

        commandBus.send(new CancelTicketOrderCommand(UUID.randomUUID(), Instant.now(),
                order.id(), order.userId(), order.eventId(), order.quantity(), "test"));

        OrderResponse cancelled = orderService.getOrder(order.id());
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(100, eventGateway.getEvent(EVENT_ID).availableCount());
        assertFalse(eventBus.published(TicketOrderCancelled.class).isEmpty());
        assertTrue(orderRepository.findById(order.id()).isPresent());
    }

    private OrderResponse reserve(int quantity, String promotionCode) {
        return createOrderUseCase.reserve(new AuthenticationContext("buyer", "127.0.0.1"),
                new CreateOrderRequest(EVENT_ID, quantity, promotionCode));
    }
}
