package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A replenishment whose event cannot be delivered must not leave the batch or the new stock
 * persisted.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryReplenishmentTransactionTest {

  @Autowired InventoryCommandService commandService;
  @Autowired InventoryRepository repository;

  @Autowired
  com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository medications;

  @MockitoBean InventoryEventPublisher eventPublisher;

  @Test
  void replenishmentIsRolledBackWhenItsEventFails() {
    var medicationId =
        medications
            .save(
                com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication.register(
                    UUID.randomUUID().toString(), "Losartan", "Tablet", java.time.Instant.now()))
            .id();
    commandService.registerInitialInventory(
        new RegisterInitialInventoryCommand(medicationId, 10, 3));
    doThrow(new IllegalStateException("listener failed"))
        .when(eventPublisher)
        .publish(any(ReplenishmentRegistered.class));

    assertThrows(
        IllegalStateException.class,
        () ->
            commandService.registerReplenishment(
                new RegisterReplenishmentCommand(medicationId, 20)));

    var inventory = repository.findByMedicationId(medicationId).orElseThrow();
    assertEquals(10, inventory.remainingStock());
    assertEquals(1, inventory.batches().size());
  }
}
