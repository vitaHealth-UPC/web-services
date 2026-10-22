package com.vitahealth.tata.inventoryreplenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryStockChanged;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * Verifies flushed source evidence and transactional rollback on synchronous publication failure.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InventoryStockPublicationIntegrationTest.Configuration.class)
class InventoryStockPublicationIntegrationTest {
  @Autowired InventoryCommandService commands;
  @Autowired InventoryRepository inventories;
  @Autowired MedicationRepository medications;
  @Autowired Clock clock;
  @Autowired Probe probe;
  @Autowired TreatmentContextFacade treatments;

  @AfterEach
  void resetProbe() {
    probe.reject = false;
    probe.events.clear();
  }

  private String medication() {
    return medications
        .save(
            Medication.register(
                UUID.randomUUID().toString(), "Losartan", "Tablet", clock.instant()))
        .id();
  }

  @Test
  void publicMedicationReservationRequiresTheCallerTransaction() {
    var medicationId = medication();
    assertThrows(
        IllegalTransactionStateException.class, () -> treatments.reserveMedication(medicationId));
  }

  @Test
  void simultaneousFirstRegistrationsProduceOneInventoryAndOneBusinessConflict() throws Exception {
    var medicationId = medication();
    var start = new CountDownLatch(1);
    Callable<String> registration =
        () -> {
          start.await();
          try {
            commands.registerInitialInventory(
                new RegisterInitialInventoryCommand(medicationId, 10, 3));
            return "REGISTERED";
          } catch (InventoryApplicationException exception) {
            return exception.code().name();
          }
        };
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first = workers.submit(registration);
      var second = workers.submit(registration);
      start.countDown();
      var outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
      assertEquals(1, outcomes.stream().filter("REGISTERED"::equals).count());
      assertEquals(1, outcomes.stream().filter("INVENTORY_ALREADY_EXISTS"::equals).count());
    }
    assertEquals(10, inventories.findByMedicationId(medicationId).orElseThrow().remainingStock());
    assertEquals(
        1,
        probe.events.stream().filter(event -> event.medicationId().equals(medicationId)).count());
  }

  @Test
  void everyActualMutationPublishesTheStoredRevisionAndDuplicateConsumptionPublishesNothing() {
    var medicationId = medication();
    commands.registerInitialInventory(new RegisterInitialInventoryCommand(medicationId, 10, 3));
    commands.registerReplenishment(new RegisterReplenishmentCommand(medicationId, 2));
    var intakeId = UUID.randomUUID().toString();
    assertTrue(commands.consumeUnit(new ConsumeUnitCommand(medicationId, intakeId)));
    assertFalse(commands.consumeUnit(new ConsumeUnitCommand(medicationId, intakeId)));
    var events =
        probe.events.stream().filter(event -> event.medicationId().equals(medicationId)).toList();
    assertEquals(
        List.of(10, 12, 11), events.stream().map(InventoryStockChanged::remainingStock).toList());
    assertTrue(events.get(1).revision() > events.get(0).revision());
    assertTrue(events.get(2).revision() > events.get(1).revision());
    assertEquals(
        inventories.findByMedicationId(medicationId).orElseThrow().revision(),
        events.getLast().revision());
  }

  @Test
  void publicationFailureRollsBackInitialStockAndAllowsRegistrationAgain() {
    var medicationId = medication();
    probe.reject = true;
    assertThrows(
        IllegalStateException.class,
        () ->
            commands.registerInitialInventory(
                new RegisterInitialInventoryCommand(medicationId, 10, 3)));
    assertTrue(inventories.findByMedicationId(medicationId).isEmpty());
    probe.reject = false;
    commands.registerInitialInventory(new RegisterInitialInventoryCommand(medicationId, 10, 3));
    assertEquals(10, inventories.findByMedicationId(medicationId).orElseThrow().remainingStock());
  }

  @Test
  void publicationFailureRollsBackReceiptRevisionAndConsumptionMarker() {
    var medicationId = medication();
    commands.registerInitialInventory(new RegisterInitialInventoryCommand(medicationId, 10, 3));
    var before = inventories.findByMedicationId(medicationId).orElseThrow();
    var intakeId = UUID.randomUUID().toString();
    probe.reject = true;
    assertThrows(
        IllegalStateException.class,
        () -> commands.registerReplenishment(new RegisterReplenishmentCommand(medicationId, 5)));
    assertThrows(
        IllegalStateException.class,
        () -> commands.consumeUnit(new ConsumeUnitCommand(medicationId, intakeId)));
    var after = inventories.findByMedicationId(medicationId).orElseThrow();
    assertEquals(before.remainingStock(), after.remainingStock());
    assertEquals(before.revision(), after.revision());
    assertEquals(before.batches().size(), after.batches().size());
    assertFalse(inventories.hasConsumed(intakeId));
    probe.reject = false;
    assertTrue(commands.consumeUnit(new ConsumeUnitCommand(medicationId, intakeId)));
  }

  /** Registers only the failure-observation listener used by this integration fixture. */
  @TestConfiguration
  static class Configuration {
    @Bean
    Probe stockPublicationProbe(InventoryRepository inventories) {
      return new Probe(inventories);
    }
  }

  /** Observes source state inside the publishing transaction before optionally rejecting it. */
  static class Probe {
    private final InventoryRepository inventories;
    final List<InventoryStockChanged> events = new ArrayList<>();
    boolean reject;

    Probe(InventoryRepository inventories) {
      this.inventories = inventories;
    }

    @EventListener
    public void observe(InventoryStockChanged event) {
      var stored = inventories.findByMedicationId(event.medicationId()).orElseThrow();
      assertEquals(stored.revision(), event.revision());
      assertEquals(stored.remainingStock(), event.remainingStock());
      assertEquals(stored.updatedAt(), event.changedAt());
      if (reject) throw new IllegalStateException("controlled stock publication failure");
      events.add(event);
    }
  }
}
