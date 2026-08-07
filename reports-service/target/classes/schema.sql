-- The only table the reporting service owns. Created idempotently on the
-- analytics cluster (a read replica of the monolith ticketwave database plus
-- this dedicated read-model table). All other tables are mapped read-only.
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
