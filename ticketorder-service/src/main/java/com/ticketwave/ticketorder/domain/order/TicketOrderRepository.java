package com.ticketwave.ticketorder.domain.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketOrderRepository {

    TicketOrder save(TicketOrder order);

    Optional<TicketOrder> findById(UUID id);

    List<TicketOrder> findByUserId(UUID userId);

    List<TicketOrder> findAll();
}
