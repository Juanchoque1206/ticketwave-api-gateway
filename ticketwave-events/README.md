# TicketWave Events

Plataforma monolítica modular (Spring Boot 4, Java 21) para gestión de eventos y venta de tickets, con un flujo unificado de **reserva + compra** basado en `TicketOrder`.

## Tecnologías

- **Java 21**
- **Spring Boot 4**
- **Spring Data JPA** + PostgreSQL (H2 para desarrollo local)
- **Spring Security + JWT** (jjwt 0.12)
- **Redis** (bloqueo de tickets y detección de fraude)
- **OpenAPI / Swagger UI**
- **Lombok**

## Requerimientos

- JDK 21
- Maven 3.9+
- PostgreSQL 15+ (o usa el perfil `local` con H2 embebida)
- Redis 7+ (opcional en perfil `local`)

## Estructura

```
ticketwave-events/
 ├── src/main/java/com/ticketwave/
 │   ├── TicketwaveApplication.java
 │   ├── config/        # Security, JWT, OpenAPI, Cache, DataSeeder
 │   ├── controller/    # Event, TicketOrder, Ticket, Payment, User, Notification, Promotion, Fraud
 │   ├── service/       # Lógica de negocio + Jobs
 │   ├── domain/        # Entidades y enums
 │   ├── repository/    # Acceso a datos
 │   ├── dto/           # Request/Response records
 │   ├── exception/     # Excepciones + GlobalExceptionHandler
 │   ├── util/          # QrCodeGenerator, PriceCalculator
 │   └── modules/       # Fronteras modulares (preparación para microservicios)
 ├── src/main/resources/  # application.yml, messages.properties
 └── src/test/
```

## Ejecución

```bash
# Desarrollo local (H2 en memoria, sin Redis)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Producción (PostgreSQL + Redis, configuración por variables de entorno)
DB_URL=jdbc:postgresql://localhost:5432/ticketwave \
DB_USERNAME=postgres DB_PASSWORD=postgres \
REDIS_HOST=localhost REDIS_PORT=6379 \
JWT_SECRET=<secreto-de-32-bytes> \
mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Credenciales de demostración (seed automático)

| Usuario | Contraseña | Rol   |
|---------|------------|-------|
| admin   | admin1234  | ADMIN |
| user    | user1234   | USER  |

## Endpoints principales

| Método | Ruta                         | Descripción                              |
|--------|------------------------------|------------------------------------------|
| GET    | `/api/events`                | Búsqueda (ciudad, artista, venue, fecha) |
| POST   | `/api/events`                | Crear evento (ADMIN)                     |
| POST   | `/api/orders`                | Crear reserva (TicketOrder)              |
| POST   | `/api/orders/{id}/cancel`    | Cancelar antes del pago                  |
| POST   | `/api/payments`              | Confirmar reserva con pago               |
| POST   | `/api/tickets/validate`      | Validar ticket en venue (ADMIN)          |
| POST   | `/api/tickets/{id}/refund`   | Reembolsar ticket                        |
| POST   | `/api/users/register`        | Registro                                 |
| POST   | `/api/users/login`           | Login → JWT                              |
| GET    | `/api/fraud/check`           | Evaluación de riesgo de fraude           |

## Flujo de adquisición (TicketOrder)

1. `POST /api/orders` → reserva temporal de tickets (bloqueo de capacidad en el evento).
2. `POST /api/payments` → pago con Stripe/PayPal; al confirmarse se emiten los tickets digitales con código QR.
3. `POST /api/orders/{id}/cancel` → cancela la reserva y libera capacidad (solo antes del pago).
4. Las órdenes `PENDING` expiran automáticamente vía `OrderExpiryJob` y liberan capacidad.

## Pruebas

```bash
mvn test
```

## Seguridad

- JWT bearer token emitido en `/api/users/login` y `/api/users/register`.
- Endpoints administrativos protegidos con `@PreAuthorize("hasRole('ADMIN')")`.
- Detección de fraude: límite de intentos por usuario/IP en Redis, prevención de órdenes duplicadas.
