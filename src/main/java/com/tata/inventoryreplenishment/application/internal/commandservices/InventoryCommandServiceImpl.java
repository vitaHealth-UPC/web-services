package com.tata.inventoryreplenishment.application.internal.commandservices;

import com.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.tata.inventoryreplenishment.application.internal.InventoryApplicationException;
import com.tata.inventoryreplenishment.application.internal.InventoryMapper;
import com.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import com.tata.inventoryreplenishment.application.models.InventoryResult;
import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class InventoryCommandServiceImpl implements InventoryCommandService {
    private final InventoryRepository repository;
    private final InventoryEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public InventoryCommandServiceImpl(InventoryRepository repository, InventoryEventPublisher eventPublisher) {
        this(repository, eventPublisher, Clock.systemUTC());
    }

    InventoryCommandServiceImpl(InventoryRepository repository, InventoryEventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public InventoryResult registerInitialInventory(RegisterInitialInventoryCommand command) {
        if (repository.existsByMedicationId(command.medicationId())) {
            throw error(InventoryApplicationException.Code.INVENTORY_ALREADY_EXISTS, "inventory already exists for this medication");
        }

        final Inventory inventory;
        try {
            inventory = Inventory.registerInitial(
                    command.medicationId(),
                    command.initialQuantity(),
                    command.replenishmentThreshold(),
                    clock.instant()
            );
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw error(InventoryApplicationException.Code.INVALID_QUANTITY, exception.getMessage());
        }
        return InventoryMapper.toResult(saveAndPublish(inventory));
    }

    @Override
    public InventoryResult registerReplenishment(RegisterReplenishmentCommand command) {
        var inventory = findInventory(command.medicationId());
        try {
            inventory.registerBatch(command.quantity(), clock.instant());
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw error(InventoryApplicationException.Code.INVALID_QUANTITY, exception.getMessage());
        }
        return InventoryMapper.toResult(saveAndPublish(inventory));
    }

    @Override
    public boolean consumeUnit(ConsumeUnitCommand command) {
        if (repository.hasConsumed(command.intakeId())) {
            return false;
        }

        var inventory = findInventory(command.medicationId());
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

    /** Events are pulled before saving because the repository returns a rehydrated copy without them. */
    private Inventory saveAndPublish(Inventory inventory) {
        var events = inventory.pullDomainEvents();
        var saved = repository.save(inventory);
        events.forEach(eventPublisher::publish);
        return saved;
    }

    private Inventory findInventory(String medicationId) {
        return repository.findByMedicationId(medicationId)
                .orElseThrow(() -> error(InventoryApplicationException.Code.INVENTORY_NOT_FOUND, "inventory not found"));
    }

    private static InventoryApplicationException error(InventoryApplicationException.Code code, String message) {
        return new InventoryApplicationException(code, message);
    }
}
