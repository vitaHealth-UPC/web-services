package com.tata.inventoryreplenishment.interfaces.rest.transform;

import com.tata.inventoryreplenishment.application.models.InventoryResult;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.tata.inventoryreplenishment.interfaces.rest.resources.BatchResource;
import com.tata.inventoryreplenishment.interfaces.rest.resources.InventoryResource;
import com.tata.inventoryreplenishment.interfaces.rest.resources.RegisterInitialInventoryResource;
import com.tata.inventoryreplenishment.interfaces.rest.resources.RegisterReplenishmentResource;

public final class InventoryResourceAssembler {
    private InventoryResourceAssembler() {}

    public static RegisterInitialInventoryCommand toCommand(RegisterInitialInventoryResource resource) {
        return new RegisterInitialInventoryCommand(
                resource.medicationId(),
                resource.initialQuantity(),
                resource.replenishmentThreshold()
        );
    }

    public static RegisterReplenishmentCommand toCommand(String medicationId, RegisterReplenishmentResource resource) {
        return new RegisterReplenishmentCommand(medicationId, resource.quantity(), resource.lot());
    }

    public static InventoryResource toResource(InventoryResult result) {
        return new InventoryResource(
                result.id(),
                result.medicationId(),
                result.remainingStock(),
                result.replenishmentThreshold(),
                result.lowStock(),
                result.batches().stream()
                        .map(batch -> new BatchResource(batch.id(), batch.quantity(), batch.registeredAt(), batch.lot()))
                        .toList(),
                result.createdAt(),
                result.updatedAt(),
                result.daysRemaining(),
                result.dailyConsumptionUnits()
        );
    }
}
