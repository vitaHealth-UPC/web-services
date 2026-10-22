package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import com.vitahealth.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.application.internal.InventoryMapper;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog;
import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryResult;
import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryEvent;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryStockChanged;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates stock changes, consumption deduplication and domain event publication. */
@Service
@Transactional
public class InventoryCommandServiceImpl implements InventoryCommandService {
  private final InventoryRepository repository;
  private final InventoryEventPublisher eventPublisher;
  private final Clock clock;
  private final MedicationCatalog medications;

  /**
   * Injects stock storage, typed event publication, medication evidence and the application clock.
   */
  public InventoryCommandServiceImpl(
      InventoryRepository repository,
      InventoryEventPublisher eventPublisher,
      MedicationCatalog medications,
      Clock clock) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
    this.medications = medications;
  }

  /** Reserves source registration before creating and publishing initial stock atomically. */
  @Override
  public InventoryResult registerInitialInventory(RegisterInitialInventoryCommand command) {
    medications.reserveForInventoryRegistration(command.medicationId());
    var availability = medications.availability(command.medicationId());
    if (availability == MedicationCatalog.Availability.MISSING) {
      throw error(InventoryApplicationException.Code.MEDICATION_NOT_FOUND, "medication not found");
    }
    if (availability == MedicationCatalog.Availability.INACTIVE) {
      throw error(InventoryApplicationException.Code.MEDICATION_INACTIVE, "medication is inactive");
    }
    if (repository.existsByMedicationId(command.medicationId())) {
      throw error(
          InventoryApplicationException.Code.INVENTORY_ALREADY_EXISTS,
          "inventory already exists for this medication");
    }

    final Inventory inventory;
    try {
      inventory =
          Inventory.registerInitial(
              command.medicationId(),
              command.initialQuantity(),
              command.replenishmentThreshold(),
              clock.instant());
    } catch (IllegalArgumentException | ArithmeticException exception) {
      throw error(InventoryApplicationException.Code.INVALID_QUANTITY, exception.getMessage());
    }
    return InventoryMapper.toResult(
        saveAndPublish(inventory), medications.dailyConsumptionUnits(inventory.medicationId()));
  }

  /** Appends a validated receipt and publishes the resulting complete stock evidence. */
  @Override
  public InventoryResult registerReplenishment(RegisterReplenishmentCommand command) {
    var inventory = findInventory(command.medicationId());
    try {
      inventory.registerBatch(command.quantity(), clock.instant(), command.lot());
    } catch (IllegalArgumentException | ArithmeticException exception) {
      throw error(InventoryApplicationException.Code.INVALID_QUANTITY, exception.getMessage());
    }
    return InventoryMapper.toResult(
        saveAndPublish(inventory), medications.dailyConsumptionUnits(inventory.medicationId()));
  }

  /** Consumes one unit under reservation while preserving intake idempotency and publication. */
  @Override
  public boolean consumeUnit(ConsumeUnitCommand command) {
    var inventory =
        repository
            .findByMedicationIdForUpdate(command.medicationId())
            .orElseThrow(
                () ->
                    error(
                        InventoryApplicationException.Code.INVENTORY_NOT_FOUND,
                        "inventory not found"));
    if (repository.hasConsumed(command.intakeId())) {
      return false;
    }

    var now = clock.instant();
    try {
      inventory.consumeUnit(now);
    } catch (IllegalStateException exception) {
      throw error(InventoryApplicationException.Code.INSUFFICIENT_STOCK, exception.getMessage());
    }

    var saved = saveAndPublish(inventory);
    repository.registerConsumption(command.intakeId(), saved.id(), now);
    return true;
  }

  /**
   * Events are pulled before saving because the repository returns a rehydrated copy without them.
   */
  private Inventory saveAndPublish(Inventory inventory) {
    var events = inventory.pullDomainEvents();
    var saved = repository.save(inventory);
    events.stream().map(InventoryEvent.class::cast).forEach(eventPublisher::publish);
    eventPublisher.publish(
        new InventoryStockChanged(
            saved.id(),
            saved.medicationId(),
            saved.remainingStock(),
            saved.replenishmentThreshold(),
            saved.revision(),
            saved.updatedAt()));
    return saved;
  }

  private Inventory findInventory(String medicationId) {
    return repository
        .findByMedicationId(medicationId)
        .orElseThrow(
            () ->
                error(
                    InventoryApplicationException.Code.INVENTORY_NOT_FOUND, "inventory not found"));
  }

  private static InventoryApplicationException error(
      InventoryApplicationException.Code code, String message) {
    return new InventoryApplicationException(code, message);
  }
}
