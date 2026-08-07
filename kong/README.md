# TicketWave — Kong API Gateway (runbook)

Kong is the **single entry point** for every TicketWave client. It terminates TLS,
authenticates centrally with the JWT issued by the app auth service, rate-limits,
logs, and exposes Prometheus metrics, then routes requests to the right backend.

```
                          ┌───────────────────────────────┐
   clients ───HTTPS──▶   │          KONG GATEWAY          │
                          │  TLS / jwt / rate-limit / log │
                          └───────┬──────────┬────────────┘
                    /api/ticketorder  /api/reports   /api/legacy
                          ▼            ▼              ▼
                  ticketorder-service  reports-service   legacy monolith
                          (8090)        (8082)          (ticketwave-events, 8081)
                          ticketorder_db read replica      ticketwave DB
                                    ▲   ▲
                                    └───┴── RabbitMQ + Redis (unchanged, behind gateway)
```

## Route table

| Kong path | Rewrite | Backend | Auth |
|---|---|---|---|
| `/api/ticketorder/**` | `/api/orders/**` | ticketorder-service :8090 | JWT (Kong) + app filter |
| `/api/reports/**` | (none) | reports-service :8082 | JWT (Kong) + app filter |
| `/api/legacy/users/login` | `/api/users/login` | monolith :8081 | **public** |
| `/api/legacy/users/register` | `/api/users/register` | monolith :8081 | **public** |
| `/api/legacy/events/**` | `/api/events/**` | monolith :8081 | **public** |
| `/api/legacy/**` (rest) | `/api/**` | monolith :8081 | JWT (Kong) + app filter |

Path rewriting is done with the `request-transformer` plugin (`replace.regex`),
so the backends keep their existing controller mappings (`/api/orders/**`,
`/api/users/**`, `/api/payments/**`, `/api/tickets/**`, …) untouched.

## JWT authentication

- The monolith's `JwtService.generateToken` now stamps `iss: ticketwave` and
  signs with the shared HS256 secret (`ticketwave.jwt.secret`, default
  `JWT_SECRET`).
- Kong's `jwt` plugin uses `key_claim_name: iss`, resolves the credential whose
  `key == "ticketwave"` and verifies the signature + `exp`.
- **The Kong credential secret MUST be identical to the apps' `JWT_SECRET`.**
- Backends keep their existing Bearer filters (defense in depth); Kong rejects
  unauthenticated traffic at the edge.

## Components

| Component | Address | Notes |
|---|---|---|
| Kong proxy | `:8000` (http) / `:8443` (https) | single entry point |
| Kong admin API | `:8001` | route/policy management |
| Kong Manager (dashboard) | `:8002` | OSS UI (enable with `KONG_ENABLE_...`) |
| Kong status/metrics | `:8100/metrics` | Prometheus plugin |
| ticketorder-service | :8090 | `ticketorder_db` PostgreSQL |
| reports-service | :8082 | read-only JPA over analytics replica of `ticketwave` + own `order_reports` table |
| legacy monolith | :8081 | `ticketwave` PostgreSQL, Redis, RabbitMQ consumer |
| RabbitMQ | :5672/:15672 | shared event bus (unchanged) |
| Redis | :6379 | saga state (unchanged) |

All Spring services must run with the **`rabbitmq`** profile so the bus adapters
are created (the default `local` profile does not provide an `EventBus` bean):

```
SPRING_PROFILES_ACTIVE=rabbitmq java -jar ticketwave-events/target/ticketwave-events-*.jar
SPRING_PROFILES_ACTIVE=rabbitmq java -jar ticketorder-service/target/ticketorder-service-*.jar
SPRING_PROFILES_ACTIVE=rabbitmq java -jar reports-service/target/reports-service-*.jar
```

## Deploying Kong

DB-less (recommended for this repo — config only):

1. Put `kong.yml` in the Kong config directory (`/etc/kong/kong.yml`) and run
   Kong with `KONG_DATABASE=off`.
2. Validate locally:
   ```
   kong config parse kong.yml
   ```
3. Reload after edits:
   ```
   kong reload
   ```

DB mode (config pushed into a Kong Postgres DB):

```
kong config db_import kong.yml
```

Docker example (DB-less, mounts this file):

```yaml
# docker-compose.yml (local, optional)
services:
  kong:
    image: kong:3.9
    environment:
      KONG_DATABASE: "off"
      KONG_DECLARATIVE_CONFIG: /kong/kong.yml
      KONG_PROXY_LISTEN: 0.0.0.0:8000, 0.0.0.0:8443 ssl
      KONG_ADMIN_LISTEN: 0.0.0.0:8001
      KONG_STATUS_LISTEN: 0.0.0.0:8100
      KONG_PLUGINS: "bundled,prometheus,request-transformer"
    volumes:
      - ./kong.yml:/kong/kong.yml:ro
    ports:
      - "8000:8000"
      - "8443:8443"
      - "8001:8001"
      - "8100:8100"
```

> `request-transformer` with `replace.regex` requires **Kong ≥ 3.4**.

## Smoke test (end to end)

```bash
# 1. Get a token (public route)
curl -s http://localhost:8000/api/legacy/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"buyer","password":"..."}'
# -> { "token": "eyJ..." }

TOKEN=eyJ...

# 2. Order lifecycle through the gateway
curl -s http://localhost:8000/api/ticketorder -X POST \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"eventId":"...","quantity":2}'

# 3. Reports (read-only)
curl -s "http://localhost:8000/api/reports/sales/overview" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8000/api/reports/sales/by-event" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8000/api/reports/orders" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8000/api/reports/payments" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8000/api/reports/tickets" -H "Authorization: Bearer $TOKEN"

# 4. Legacy endpoints, unchanged mapping
curl -s http://localhost:8000/api/legacy/tickets/validate -X POST \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"qrCode":"..."}'

# 5. Unauthenticated -> 401 from Kong
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8000/api/reports/orders   # 401
```

## Monitoring & logging

- Metrics: `GET :8100/metrics` (Prometheus scrape target) — request counts,
  latencies, bandwidth per route/consumer.
- Logs: the `http-log` plugin forwards every request as JSON to the configured
  collector (`http://logs-collector:8080/kong` — replace with your log pipeline,
  e.g. Loki/ELK). `custom_fields_by_lua` enriches entries with service/route/consumer.
- Kong admin API (`:8001`) can be used to inspect services/routes/plugins
  (`GET /services`, `GET /routes`, `GET /plugins`).

## TLS

Replace the placeholder certificate in `kong.yml`:

```bash
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem \
  -days 365 -nodes -subj "/CN=api.ticketwave.local"
```

Paste the PEM blocks into the `certificates` section (SNI `api.ticketwave.local`).
Point DNS/`/etc/hosts` at the gateway host and call `https://api.ticketwave.local/api/...`.
Without TLS (plain `http` routes) the cert section can be left empty.

## Operational notes / caveats

- **Bus fan-out**: the monolith and ticketorder-service both bind the same queue
  `ticketwave.events.all`, so when both run their RabbitMQ consumers share one
  queue (competing consumers). The reports-service deliberately binds its own
  queue `ticketwave.events.reports` so it always receives a full copy of every
  event. If a single copy per service is required for the other two, give each
  service its own queue name.
- **Refund accumulation**: `order_reports.refunded_amount` is accumulated from
  `TicketRefunded` events. RabbitMQ delivery is at-least-once, so a redelivered
  event can inflate the refund total. Production should add an idempotency table
  keyed on event id before processing.
- **Read-only discipline**: reports-service maps the monolith tables
  (`payments`, `tickets`, `notifications`, `events`, `app_users`) as `@Immutable`
  projections and uses `ddl-auto: none`; its only write is its own `order_reports`
  table (created by `schema.sql`). Point `REPORTS_DB_URL` at a read replica, not
  the primary monolith DB.
- **Secrets**: change `JWT_SECRET` in every service AND the matching credential
  secret in `kong.yml`. Never use the placeholder in production.



# docker kong
docker run -d --name kong  -e "KONG_DATABASE=off"   -e "KONG_DECLARATIVE_CONFIG=/kong.yml"   -p 8000:8000   -p 8001:8001   kong:latest