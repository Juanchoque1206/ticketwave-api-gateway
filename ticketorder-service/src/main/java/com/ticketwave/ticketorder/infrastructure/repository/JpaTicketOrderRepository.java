package com.ticketwave.ticketorder.infrastructure.repository;

import com.ticketwave.ticketorder.domain.order.TicketOrder;
import com.ticketwave.ticketorder.domain.order.TicketOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaTicketOrderRepository extends TicketOrderRepository, JpaRepository<TicketOrder, UUID> {
}
