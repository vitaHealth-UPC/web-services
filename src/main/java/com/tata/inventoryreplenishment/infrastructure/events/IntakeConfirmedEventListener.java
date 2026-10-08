package com.tata.inventoryreplenishment.infrastructure.events;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.inventoryreplenishment.interfaces.events.IntakeConfirmedEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("inventoryIntakeConfirmedEventListener")
public class IntakeConfirmedEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntakeConfirmedEventListener.class);
    private final IntakeConfirmedEventConsumer consumer;
    public IntakeConfirmedEventListener(IntakeConfirmedEventConsumer consumer) { this.consumer = consumer; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IntakeConfirmed event) {
        try {
            consumer.consume(event);
        } catch (RuntimeException exception) {
            LOGGER.warn("Inventory consumption failed for intake {}: {}", event.intakeId(), exception.getClass().getSimpleName());
        }
    }
}
