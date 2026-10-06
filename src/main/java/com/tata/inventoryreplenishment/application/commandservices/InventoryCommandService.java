package com.tata.inventoryreplenishment.application.commandservices;

import com.tata.inventoryreplenishment.application.models.InventoryResult;
import com.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;

public interface InventoryCommandService {
    InventoryResult registerInitialInventory(RegisterInitialInventoryCommand command);
    InventoryResult registerReplenishment(RegisterReplenishmentCommand command);

    /** Returns false when the unit for that intake was already consumed (idempotent retry). */
    boolean consumeUnit(ConsumeUnitCommand command);
}
