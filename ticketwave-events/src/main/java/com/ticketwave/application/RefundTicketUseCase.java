package com.ticketwave.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.TicketRefunded;
import com.ticketwave.domain.ticket.Ticket;
import com.ticketwave.domain.ticket.TicketRepository;
import com.ticketwave.infrastructure.dto.ticket.TicketResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefundTicketUseCase {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final EventBus eventBus;

    public RefundTicketUseCase(TicketService ticketService,
                               TicketRepository ticketRepository,
                               EventBus eventBus) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.eventBus = eventBus;
    }

    @Transactional
    public TicketResponse refund(UUID id) {
        TicketResponse response = ticketService.refund(id);
        ticketRepository.findById(id)
                .ifPresent(ticket -> eventBus.publish(new TicketRefunded(
                        UUID.randomUUID(),
                        Instant.now(),
                        ticket.getId(),
                        ticket.getOrderId(),
                        ticket.getUserId(),
                        response.price())));
        return response;
    }
}