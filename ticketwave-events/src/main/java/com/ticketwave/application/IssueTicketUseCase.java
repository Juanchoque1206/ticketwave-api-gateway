package com.ticketwave.application;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.commands.IssueTicketCommand;
import com.ticketwave.domain.event.Event;
import com.ticketwave.domain.event.EventRepository;
import com.ticketwave.domain.events.TicketDeliveryFailed;
import com.ticketwave.domain.events.TicketIssued;
import com.ticketwave.domain.ticket.Ticket;
import com.ticketwave.domain.ticket.TicketRepository;
import com.ticketwave.domain.ticket.TicketStatus;
import com.ticketwave.infrastructure.exception.ResourceNotFoundException;
import com.ticketwave.infrastructure.util.QrCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executes the ticket issuance step of the saga: emits one digital ticket per
 * seat for the given order, publishing TicketIssued (or TicketDeliveryFailed on
 * any error so the orchestrator can compensate).
 * <p>
 * The order aggregate lives in the ticketorder-service, so tickets reference it
 * by scalar orderId and carry the userId supplied by the command.
 */
@Service
public class IssueTicketUseCase {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final EventBus eventBus;

    public IssueTicketUseCase(TicketRepository ticketRepository,
                              EventRepository eventRepository,
                              EventBus eventBus,
                              CommandBus commandBus) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.eventBus = eventBus;
        commandBus.subscribe(IssueTicketCommand.class, this::issue);
    }

    @Transactional
    public void issue(IssueTicketCommand command) {
        try {
            Event event = eventRepository.findById(command.eventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

            List<UUID> ticketIds = new ArrayList<>();
            for (int i = 0; i < command.quantity(); i++) {
                Ticket ticket = new Ticket();
                ticket.setOrderId(command.orderId());
                ticket.setUserId(command.userId());
                ticket.setEvent(event);
                ticket.setPrice(event.getBasePrice());
                ticket.setSeat("Row-" + (i + 1));
                ticket.setStatus(TicketStatus.EMITTED);
                ticket.setIssuedAt(LocalDateTime.now());
                ticket.setQrCode(QrCodeGenerator.generate(
                        command.orderId().toString(), UUID.randomUUID().toString(), event.getId().toString()));
                ticketIds.add(ticketRepository.save(ticket).getId());
            }

            eventBus.publish(new TicketIssued(UUID.randomUUID(), Instant.now(),
                    command.orderId(), command.userId(), command.eventId(), ticketIds));
        } catch (Exception ex) {
            eventBus.publish(new TicketDeliveryFailed(UUID.randomUUID(), Instant.now(),
                    command.orderId(), command.userId(), ex.getMessage()));
        }
    }
}
