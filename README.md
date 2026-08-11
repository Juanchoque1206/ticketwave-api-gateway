# TicketWave Events

Modular monolith platform (Spring Boot 4, Java 21) for event management and ticket sales, with a unified **reserve + purchase** flow based on `TicketOrder`.

## Technologies

- **Java 21**
- **Spring Boot 4**
- **Spring Data JPA** + PostgreSQL (H2 for local development)
- **Spring Security + JWT** (jjwt 0.12)
- **Redis** (ticket locking and fraud detection)
- **OpenAPI / Swagger UI**
- **Lombok**

## Requirements

- JDK 21
- Maven 3.9+
- PostgreSQL 15+ (or use the `local` profile with embedded H2)
- Redis 7+ (optional in the `local` profile)
- Docker (for external dependencies)

## Docker (external dependencies)

Independent containers (you can start them separately or together):

```bash
# rabbitmq
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management

# redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# kong api gateway
docker run -d --name kong \
  -e "KONG_DATABASE=off" \
  -e "KONG_DECLARATIVE_CONFIG=/kong/kong.yml" \
  -e "KONG_ADMIN_LISTEN=0.0.0.0:8001" \
  -v ticketwave-api-gateway\ticketwave-api-gateway\kong:/kong \
  -p 9000:8000 \
  -p 9001:8001 \
  kong:latest

# Prometheus
docker run -d --name prometheus \
  -v ticketwave-api-gateway\ticketwave-api-gateway\prometheus\prometheus.yml:/etc/prometheus/prometheus.yml \
  -p 9090:9090 prom/prometheus

# grafana
docker run -d --name grafana -p 3000:3000 grafana/grafana
```

External links:

- RabbitMQ Management: <http://localhost:15672/>
- Prometheus Targets: <http://localhost:9090/targets>
- Grafana: <http://localhost:3000/>

## Kong (API Gateway) configuration

Kong runs in DB-less mode reading the declarative config from `kong/kong.yml` (mounted as `/kong/kong.yml` in the container). Services, routes, and plugins:

```yaml
_format_version: "3.0"
_transform: true

services:
  - name: service-8091
    url: http://host.docker.internal:8091
    routes:
      - name: route-8091
        paths:
          - /api8091
        strip_path: true
      - name: route-8091-docs
        paths:
          - /v3/api-docs/reports
        strip_path: false
      - name: route-8091-swagger-ui
        paths:
          - /reports
        strip_path: false

  - name: service-8090
    url: http://host.docker.internal:8090
    routes:
      - name: route-8090
        paths:
          - /api8090
        strip_path: true
      - name: route-8090-docs
        paths:
          - /v3/api-docs/ticketorder
        strip_path: false
      - name: route-8090-swagger-ui
        paths:
          - /ticketorder
        strip_path: false

  - name: service-8081
    url: http://host.docker.internal:8081
    routes:
      - name: route-8081
        paths:
          - /api8081
        strip_path: true
      - name: route-8081-docs
        paths:
          - /v3/api-docs/legacy
        strip_path: false
      - name: route-8081-swagger-ui
        paths:
          - /legacy
        strip_path: false

plugins:
  - name: prometheus
  
  - name: http-log
    config:
      http_endpoint: http://host.docker.internal:8085/logs
      method: POST
      timeout: 1000
      keepalive: 30
      flush_timeout: 2


  # - name: jwt
  #   config:
  #     key_claim_name: kid
  #     secret_is_base64: false

# ---------------------------------------------------------------------------
# TLS termination
# ---------------------------------------------------------------------------
# Self-signed placeholder cert for local testing. Generate a real one with:
#   openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem \
#       -days 365 -nodes -subj "/CN=api.ticketwave.local"
# and paste the PEM blocks below (replace the placeholders).
# certificates:
#   - id: ticketwave-tls
#     cert: |
#       -----BEGIN CERTIFICATE-----
#       (replace with your certificate PEM - see README)
#       -----END CERTIFICATE-----
#     key: |
#       -----BEGIN PRIVATE KEY-----
#       (replace with your private key PEM - see README)
#       -----END PRIVATE KEY-----
#     snis:
#       - name: api.ticketwave.local
```

## Prometheus configuration

Prometheus reads its config from `prometheus/prometheus.yml` (mounted as `/etc/prometheus/prometheus.yml` in the container):

```yaml
scrape_configs:
  - job_name: 'kong'
    metrics_path: /metrics
    static_configs:
      - targets: ['host.docker.internal:9001']
```

### Examples: direct access vs API gateway

Access data directly from the service or through the Kong API gateway:

```bash
# Direct access to the ticketorder service
curl http://localhost:8090/api/orders

# Access through the Kong API gateway
curl http://localhost:9000/api8090/api/orders
```

## Structure

```
ticketwave-events/
 ├── src/main/java/com/ticketwave/
 │   ├── TicketwaveApplication.java
 │   ├── config/        # Security, JWT, OpenAPI, Cache, DataSeeder
 │   ├── controller/    # Event, TicketOrder, Ticket, Payment, User, Notification, Promotion, Fraud
 │   ├── service/       # Business logic + Jobs
 │   ├── domain/        # Entities and enums
 │   ├── repository/    # Data access
 │   ├── dto/           # Request/Response records
 │   ├── exception/     # Exceptions + GlobalExceptionHandler
 │   ├── util/          # QrCodeGenerator, PriceCalculator
 │   └── modules/       # Modular boundaries (preparation for microservices)
 ├── src/main/resources/  # application.yml, messages.properties
 └── src/test/
```

## Running

```bash
# Local development (in-memory H2, no Redis)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Production (PostgreSQL + Redis, configured via environment variables)
DB_URL=jdbc:postgresql://localhost:5432/ticketwave \
DB_USERNAME=postgres DB_PASSWORD=postgres \
REDIS_HOST=localhost REDIS_PORT=6379 \
JWT_SECRET=<32-byte-secret> \
mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Swagger docs

### Direct swagger docs

- legacy: <http://localhost:8081/legacy/swagger-ui/index.html>
- ticketorder: <http://localhost:8090/ticketorder/swagger-ui/index.html>
- reports: <http://localhost:8091/reports/swagger-ui/index.html>

### Api gateway (kong) swagger docs

- <http://localhost:9000/legacy/swagger-ui/index.html>
- <http://localhost:8090/ticketorder/swagger-ui/index.html>
- <http://localhost:9000/reports/swagger-ui/index.html>

## Diagrams

System architecture and data model diagrams (open the `.svg` files in a browser or the `.drawio` sources in [draw.io](https://app.diagrams.net/)):

### C4 model (system architecture)

- Context (C1): `diagrams\c4model\ticketwave-c1-context.drawio.svg`
- Containers (C2): `diagrams\c4model\ticketwave-c2-container_copy.drawio.svg`

### Database diagram

- ER model: `diagrams\db\ticketwave-er-full.drawio.svg`

## Demo credentials (automatic seed)

| User   | Password | Role  |
|--------|----------|-------|
| admin  | admin1234 | ADMIN |
| user   | user1234  | USER  |

## Main endpoints

| Method | Route                         | Description                              |
|--------|-------------------------------|------------------------------------------|
| GET    | `/api/events`                 | Search (city, artist, venue, date)       |
| POST   | `/api/events`                 | Create event (ADMIN)                     |
| POST   | `/api/orders`                 | Create reservation (TicketOrder)         |
| POST   | `/api/orders/{id}/cancel`     | Cancel before payment                    |
| POST   | `/api/payments`               | Confirm reservation with payment         |
| POST   | `/api/tickets/validate`       | Validate ticket at venue (ADMIN)         |
| POST   | `/api/tickets/{id}/refund`    | Refund ticket                            |
| POST   | `/api/users/register`         | Register                                 |
| POST   | `/api/users/login`            | Login → JWT                              |
| GET    | `/api/fraud/check`            | Fraud risk assessment                    |

## Purchase flow (TicketOrder)

1. `POST /api/orders` → temporary ticket reservation (locks event capacity).
2. `POST /api/payments` → payment via Stripe/PayPal; on confirmation, digital QR-code tickets are issued.
3. `POST /api/orders/{id}/cancel` → cancels the reservation and releases capacity (only before payment).
4. `PENDING` orders expire automatically via `OrderExpiryJob` and release capacity.

## Tests

```bash
mvn test
```

## Security

- JWT bearer token issued at `/api/users/login` and `/api/users/register`.
- Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`.
- Fraud detection: per-user/IP attempt limits in Redis, prevention of duplicate orders.
