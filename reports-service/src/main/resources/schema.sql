-- The only table the reporting service owns. Created idempotently on the
-- analytics cluster (a read replica of the monolith ticketwave database plus
-- this dedicated read-model table). All other tables are mapped read-only
-- projections over the monolith tables and are created idempotently as well.
CREATE TABLE IF NOT EXISTS order_reports (
    order_id                  UUID PRIMARY KEY,
    user_id                   UUID NOT NULL,
    event_id                  UUID NOT NULL,
    event_name                VARCHAR(150),
    quantity                  INTEGER NOT NULL,
    total_amount              NUMERIC(10, 2) NOT NULL,
    discount_amount           NUMERIC(10, 2) NOT NULL,
    status                    VARCHAR(20) NOT NULL,
    reserved_at               TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    paid_at                   TIMESTAMPTZ,
    payment_status            VARCHAR(20),
    provider_transaction_id   VARCHAR(100),
    ticket_count              INTEGER NOT NULL DEFAULT 0,
    refunded_amount           NUMERIC(10, 2) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_report_user ON order_reports (user_id);
CREATE INDEX IF NOT EXISTS idx_order_report_event ON order_reports (event_id);
CREATE INDEX IF NOT EXISTS idx_order_report_status ON order_reports (status);
CREATE INDEX IF NOT EXISTS idx_order_report_reserved ON order_reports (reserved_at);

CREATE TABLE IF NOT EXISTS app_users (
    id          UUID PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(120),
    full_name   VARCHAR(255),
    city        VARCHAR(100),
    role        VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS events (
    id             UUID PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    artist         VARCHAR(255),
    city           VARCHAR(100),
    event_date     TIMESTAMPTZ  NOT NULL,
    base_price     NUMERIC(10, 2) NOT NULL,
    total_capacity INTEGER      NOT NULL,
    reserved_count INTEGER      NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    channel    VARCHAR(20)  NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    body       VARCHAR(2000),
    read       BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications (created_at);

CREATE TABLE IF NOT EXISTS payments (
    id                      UUID PRIMARY KEY,
    order_id                UUID         NOT NULL,
    provider                VARCHAR(20)  NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    amount                  NUMERIC(10, 2) NOT NULL,
    provider_transaction_id VARCHAR(100),
    paid_at                 TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_payment_paid ON payments (paid_at);
CREATE INDEX IF NOT EXISTS idx_payment_status_paid ON payments (status, paid_at);

CREATE TABLE IF NOT EXISTS tickets (
    id            UUID PRIMARY KEY,
    qr_code       VARCHAR(64)  NOT NULL UNIQUE,
    order_id      UUID         NOT NULL,
    user_id       UUID         NOT NULL,
    event_id      UUID         NOT NULL,
    price         NUMERIC(10, 2) NOT NULL,
    seat          VARCHAR(50),
    status        VARCHAR(20)  NOT NULL,
    issued_at     TIMESTAMPTZ,
    validated_at  TIMESTAMPTZ,
    refunded_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ticket_issued ON tickets (issued_at);
CREATE INDEX IF NOT EXISTS idx_ticket_status_issued ON tickets (status, issued_at);
