# C3 — Component diagram: cross-service ticket-purchase saga

Mermaid diagram (C4 level 3, components) showing how **ticketwave-events**
(monolith, port 8081) and **ticketorder-service** (port 8090) collaborate over
RabbitMQ to run the **orchestrated saga** Order → Payment → Ticket → Notification.

```mermaid
C4Component
title C3 — Component diagram: orchestrated purchase saga across ticketwave-events and ticketorder-service (RabbitMQ)

Person customer "Customer" "Buys tickets through the web/mobile client"

System_Ext rabbit "RabbitMQ" "Shared bus<br/>Topic exchanges: ticketwave.events / ticketwave.commands<br/>Durable queues: ticketwave.events.all / ticketwave.commands.all<br/>Routing: message class name, queues bound with #"
System_Ext redis "Redis" "Saga snapshot store (RedisSagaStateRepository, hash + TTL)"
System_Ext db_mono "PostgreSQL (monolith)" "Event, Promotion, User, Payment, Ticket, Notification"
System_Ext db_order "PostgreSQL (order svc)" "ticket_orders, order_items"

System_Boundary mono "ticketwave-events — monolith (port 8081)" {
  Container_Boundary mono_saga "Saga orchestration" {
    Component orchestrator "TicketOrderSagaOrchestrator" "Starts saga, persists each step, drives steps, compensates"
    Component sagarepo "SagaStateRepository" "Redis snapshot per order, resumable"
  }
  Container_Boundary mono_cmd "Command handlers" {
    Component confirmPayment "ConfirmOrderUseCase" "ProcessPaymentCommand handler; legacy REST confirm"
    Component paymentSvc "PaymentService" "create / refund; RefundPaymentCommand handler"
    Component issueTicket "IssueTicketUseCase" "IssueTicketCommand handler; emits tickets"
    Component notifySub "NotificationEventSubscriber" "NotifyOrderCommand handler + lifecycle notifications"
  }
  Container_Boundary mono_evt "Event subscribers" {
    Component paySub "PaymentEventSubscriber" "Capture / void / refund side effects"
    Component venueSub "VenueEventSubscriber" "Venue holds / releases"
  }
  Container_Boundary mono_int "Internal REST API (X-Internal-Token)" {
    Component evApi "EventController" "GET /{id} · POST /{id}/reserve · POST /{id}/release"
    Component promoApi "PromotionController" "POST /{code}/quote · /increment-usage"
    Component userApi "UserController" "GET /by-username/{username}"
    Component fraudApi "FraudController" "POST /guard · POST /orders"
  }
  Container_Boundary mono_bus "Bus adapters (@Profile rabbitmq)" {
    Component monoEvt "RabbitMQEventBusAdapter"
    Component monoCmd "RabbitMQCommandBusAdapter"
  }
  Component monoOrderInfo "OrderInfoClient" "REST GET /api/orders/{id} (legacy confirm fallback)"
}

System_Boundary order "ticketorder-service (port 8090)" {
  Container_Boundary order_api "REST API (JWT)" {
    Component orderCtrl "TicketOrderController" "POST /api/orders · GET /{id} · POST /{orderId}/cancel"
  }
  Container_Boundary order_app "Application" {
    Component createUC "CreateOrderUseCase" "Persist PENDING + publish TicketOrderCreated"
    Component orderSvc "OrderService" "createReservation / confirm / cancel / expire (TTL)"
    Component cancelUC "CancelOrderUseCase" "Cancel + release + publish TicketOrderCancelled"
    Component confirmOnPay "ConfirmOrderOnPayment" "on PaymentAuthorized → CONFIRMED → TicketOrderConfirmed"
    Component cancelOnCmd "CancelOrderOnCommand" "on CancelTicketOrderCommand → cancel (compensation)"
    Component expiry "OrderExpiryJob" "Expire stale PENDING orders"
  }
  Container_Boundary order_dom "Order aggregate (JPA)" {
    Component ticketOrder "TicketOrder / OrderItem / PriceCalculator"
    Component orderRepo "TicketOrderRepository (Jpa)"
  }
  Container_Boundary order_ports "Hexagonal ports" {
    Component evGate "EventGateway"
    Component promoGate "PromotionGateway"
    Component userGate "UserGateway"
    Component fraudGate "FraudGateway"
  }
  Container_Boundary order_rest "REST clients (X-Internal-Token)" {
    Component restEv "RestEventGateway"
    Component restPromo "RestPromotionGateway"
    Component restUser "RestUserGateway"
    Component restFraud "RestFraudGateway"
  }
  Container_Boundary order_bus "Bus adapters (@Profile rabbitmq)" {
    Component orderEvt "RabbitMQEventBusAdapter"
    Component orderCmd "RabbitMQCommandBusAdapter"
  }
}

Rel(customer, orderCtrl, "POST /api/orders (Bearer JWT)")

Rel(orderCtrl, createUC, "reserve()")
Rel(createUC, orderSvc, "createReservation()")
Rel(orderSvc, userGate, "findByUsername")
Rel(orderSvc, fraudGate, "guard / markOrder")
Rel(orderSvc, evGate, "getEvent / reserveCapacity")
Rel(orderSvc, promoGate, "quote / incrementUsage")
Rel(userGate, restUser, "REST")
Rel(fraudGate, restFraud, "REST")
Rel(evGate, restEv, "REST")
Rel(promoGate, restPromo, "REST")
Rel(restUser, userApi, "GET /by-username/{username}")
Rel(restFraud, fraudApi, "POST /guard · /orders")
Rel(restEv, evApi, "GET /{id} · POST /{id}/reserve")
Rel(restPromo, promoApi, "POST /{code}/quote · /increment-usage")

Rel(createUC, orderEvt, "publish TicketOrderCreated")
Rel(orderEvt, rabbit, "events exchange")
Rel(rabbit, monoEvt, "TicketOrderCreated · PaymentAuthorized · PaymentFailed · TicketIssued · TicketDeliveryFailed · NotificationSent · NotificationFailed · TicketRefunded · TicketOrderCancelled · TicketOrderConfirmed")

Rel(monoEvt, orchestrator, "subscribe saga lifecycle events")
Rel(monoEvt, paySub, "subscribe PaymentAuthorized / TicketOrderCancelled / TicketRefunded")
Rel(monoEvt, venueSub, "subscribe TicketOrderCreated / TicketOrderCancelled")
Rel(monoEvt, notifySub, "subscribe TicketOrderCreated / PaymentFailed / TicketOrderCancelled / TicketRefunded")

Rel(orchestrator, sagarepo, "save / progress / find")
Rel(sagarepo, redis, "hash + TTL")
Rel(orchestrator, monoCmd, "send ProcessPayment / IssueTicket / NotifyOrder / CancelTicketOrder / RefundPayment")
Rel(monoCmd, rabbit, "commands exchange")
Rel(rabbit, orderCmd, "CancelTicketOrderCommand (compensation)")
Rel(orderCmd, cancelOnCmd, "onCancel")
Rel(cancelOnCmd, cancelUC, "cancel()")
Rel(cancelUC, orderSvc, "cancel → CANCELLED + release capacity")
Rel(cancelUC, orderEvt, "publish TicketOrderCancelled")

Rel(monoCmd, confirmPayment, "ProcessPaymentCommand")
Rel(confirmPayment, paymentSvc, "create(orderId, provider, amount)")
Rel(confirmPayment, monoEvt, "publish PaymentAuthorized / PaymentFailed")
Rel(rabbit, orderEvt, "PaymentAuthorized")
Rel(orderEvt, confirmOnPay, "onPaymentAuthorized")
Rel(confirmOnPay, orderSvc, "confirm(orderId) → CONFIRMED")
Rel(confirmOnPay, orderEvt, "publish TicketOrderConfirmed")

Rel(monoCmd, issueTicket, "IssueTicketCommand")
Rel(issueTicket, monoEvt, "publish TicketIssued / TicketDeliveryFailed")
Rel(monoCmd, notifySub, "NotifyOrderCommand")
Rel(notifySub, monoEvt, "publish NotificationSent / NotificationFailed")
Rel(monoCmd, paymentSvc, "RefundPaymentCommand")
Rel(paymentSvc, monoEvt, "publish TicketRefunded")
Rel(orchestrator, monoEvt, "publish TicketOrderCompleted")

Rel(monoOrderInfo, orderCtrl, "GET /api/orders/{orderId} (legacy confirm)")
Rel(orderSvc, orderRepo, "persist aggregate")
Rel(orderRepo, db_order, "SQL")
Rel(orchestrator, db_mono, "reads Event / User aggregates")
```

## Saga flow (orchestration, in sequence)

1. **Create** — `POST /api/orders` → `OrderService.createReservation()` validates the
   user (UserGateway), fraud-guards it, reserves event capacity (EventGateway),
   optionally quotes a promotion, persists the order as `PENDING`, then
   `CreateOrderUseCase` publishes **`TicketOrderCreated`**.
2. **Start saga** — the monolith `TicketOrderSagaOrchestrator` receives
   `TicketOrderCreated`, snapshots the saga (Redis) and sends **`ProcessPaymentCommand`**.
3. **Pay** — `ConfirmOrderUseCase` charges (STRIPE/PAYPAL), publishes
   **`PaymentAuthorized`**; on failure it publishes `PaymentFailed`.
   - In parallel, order-service `ConfirmOrderOnPayment` consumes `PaymentAuthorized`
     and advances the order to `CONFIRMED`, publishing **`TicketOrderConfirmed`**.
4. **Issue tickets** — the orchestrator sends `IssueTicketCommand`;
   `IssueTicketUseCase` emits the tickets and publishes **`TicketIssued`**.
5. **Notify** — the orchestrator sends `NotifyOrderCommand`;
   `NotificationEventSubscriber` sends the purchase-completed notification and
   publishes **`NotificationSent`**.
6. **Complete** — the orchestrator marks the saga `COMPLETED` and publishes
   **`TicketOrderCompleted`**.

## Compensation

- `PaymentFailed` → orchestrator sends **`CancelTicketOrderCommand`** →
  `CancelOrderOnCommand` → `CancelOrderUseCase` cancels the order, releases
  capacity via `EventGateway`, publishes `TicketOrderCancelled`.
- `TicketDeliveryFailed` → orchestrator sends **`RefundPaymentCommand`** →
  `PaymentService.refundPayment` → publishes `TicketRefunded`.
- Stale `PENDING` orders (no payment within TTL) are cancelled by
  `OrderExpiryJob` in order-service.

## Notes

- **Shared contract by FQCN**: both services keep identical records under
  `com.ticketwave.domain.events.*` / `com.ticketwave.domain.commands.*`; the
  Jackson polymorphic validator only allows those two packages, so a message
  published by one service deserializes into the other's matching type.
- **Bus topology**: events on `ticketwave.events` (queue `ticketwave.events.all`),
  commands on `ticketwave.commands` (queue `ticketwave.commands.all`), routing key `#`.
- **Order service owns the order aggregate**; the monolith only observes it through
  events/commands and calls it over REST (`OrderInfoClient`) for the legacy confirm path.
- **Test profile**: in-memory `EventBus`/`CommandBus`/`SagaStateRepository` replace
  RabbitMQ/Redis so the saga runs end-to-end offline.
