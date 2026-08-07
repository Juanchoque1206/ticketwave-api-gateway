package com.ticketwave.application;

import com.ticketwave.domain.ticket.Ticket;
import com.ticketwave.domain.ticket.TicketStatus;
import com.ticketwave.infrastructure.dto.ticket.TicketResponse;
import com.ticketwave.infrastructure.dto.ticket.ValidateTicketResponse;
import com.ticketwave.infrastructure.exception.BusinessRuleException;
import com.ticketwave.infrastructure.exception.ResourceNotFoundException;
import com.ticketwave.domain.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public TicketResponse get(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listByOrder(UUID orderId) {
        return ticketRepository.findByOrderId(orderId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ValidateTicketResponse validate(String qrCode) {
        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() == TicketStatus.EMITTED) {
            ticket.setStatus(TicketStatus.VALIDATED);
            ticket.setValidatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
            return new ValidateTicketResponse(ticket.getQrCode(), ticket.getEvent().getName(),
                    ticket.getSeat(), ticket.getStatus(), true, "Valid ticket");
        }
        return new ValidateTicketResponse(ticket.getQrCode(), ticket.getEvent().getName(),
                ticket.getSeat(), ticket.getStatus(), false, "Ticket already used or not valid");
    }

    @Transactional
    public TicketResponse refund(UUID id) {
        Ticket ticket = getEntity(id);
        if (ticket.getStatus() != TicketStatus.EMITTED && ticket.getStatus() != TicketStatus.VALIDATED) {
            throw new BusinessRuleException("Ticket cannot be refunded");
        }
        ticket.setStatus(TicketStatus.REFUNDED);
        ticket.setRefundedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    public Ticket getEntity(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getQrCode(), ticket.getOrderId(),
                ticket.getEvent().getId(), ticket.getEvent().getName(), ticket.getPrice(),
                ticket.getSeat(), ticket.getStatus(), ticket.getIssuedAt());
    }
}