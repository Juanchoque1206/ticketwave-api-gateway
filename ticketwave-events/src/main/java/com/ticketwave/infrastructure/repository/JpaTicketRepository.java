package com.ticketwave.infrastructure.repository;

import com.ticketwave.domain.ticket.Ticket;
import com.ticketwave.domain.ticket.TicketRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaTicketRepository extends TicketRepository, JpaRepository<Ticket, UUID> {
}