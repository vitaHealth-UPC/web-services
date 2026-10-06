package com.tata.inventoryreplenishment.application.internal.commandservices;

import com.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import com.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/** A replenishment whose event cannot be delivered must not leave the batch or the new stock persisted. */
@SpringBootTest
@ActiveProfiles("test")
class InventoryReplenishmentTransactionTest {

    @Autowired InventoryCommandService commandService;
    @Autowired InventoryRepository repository;
    @MockitoBean InventoryEventPublisher eventPublisher;

    @Test
    void replenishmentIsRolledBackWhenItsEventFails() {
        var medicationId = UUID.randomUUID().toString();
        commandService.registerInitialInventory(new RegisterInitialInventoryCommand(medicationId, 10, 3));
        doThrow(new IllegalStateException("listener failed"))
                .when(eventPublisher).publish(any(ReplenishmentRegistered.class));

        assertThrows(
                IllegalStateException.class,
                () -> commandService.registerReplenishment(new RegisterReplenishmentCommand(medicationId, 20))
        );

        var inventory = repository.findByMedicationId(medicationId).orElseThrow();
        assertEquals(10, inventory.remainingStock());
        assertEquals(1, inventory.batches().size());
    }
}
