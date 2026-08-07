package com.ticketwave.reports.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model materialized from the shared event bus. One row per ticket order,
 * upserted by {@code OrderReportProjector} every time a lifecycle event arrives.
 * This is the only table the reporting service writes to; it lives in its own
 * schema (created by {@code schema.sql}) on the analytics cluster.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_reports", indexes = {
        @Index(name = "idx_order_report_user", columnList = "user_id"),
        @Index(name = "idx_order_report_event", columnList = "event_id"),
        @Index(name = "idx_order_report_status", columnList = "status"),
        @Index(name = "idx_order_report_reserved", columnList = "reserved_at")
})
public class OrderReport {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_name", length = 150)
    private String eventName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "ticket_count", nullable = false)
    private int ticketCount;

    @Column(name = "refunded_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
}
