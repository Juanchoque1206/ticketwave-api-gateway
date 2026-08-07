package com.ticketwave.ticketorder.application;

import com.ticketwave.domain.bus.CommandBus;
import com.ticketwave.domain.commands.CancelTicketOrderCommand;
import org.springframework.stereotype.Component;

/**
 * Compensating step of the saga: when payment fails the orchestrator sends
 * CancelTicketOrderCommand, which is turned into the same cancel + release +
 * publish flow as the user-facing REST cancellation.
 */
@Component
public class CancelOrderOnCommand {

    private final CancelOrderUseCase cancelOrderUseCase;

    public CancelOrderOnCommand(CommandBus commandBus, CancelOrderUseCase cancelOrderUseCase) {
        this.cancelOrderUseCase = cancelOrderUseCase;
        commandBus.subscribe(CancelTicketOrderCommand.class, this::onCancel);
    }

    private void onCancel(CancelTicketOrderCommand command) {
        cancelOrderUseCase.cancel(command.orderId());
    }
}
