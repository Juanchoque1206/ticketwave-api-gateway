package com.ticketwave.domain.ticket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {

    Optional<Ticket> findByQrCode(String qrCode);

    List<Ticket> findByOrderId(UUID orderId);

    Optional<Ticket> findById(UUID id);

    Ticket save(Ticket ticket);
}