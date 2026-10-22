package com.vitahealth.tata.inventoryreplenishment.application.commandservices;

import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryResult;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;

/** Coordinates transactional stock registration, receipts and confirmed intake consumption. */
public interface InventoryCommandService {
  /** Creates the medication inventory once and publishes its complete source evidence. */
  InventoryResult registerInitialInventory(RegisterInitialInventoryCommand command);

  /** Appends a receipt and returns current stock with available coverage evidence. */
  InventoryResult registerReplenishment(RegisterReplenishmentCommand command);

  /** Returns false when the unit for that intake was already consumed (idempotent retry). */
  boolean consumeUnit(ConsumeUnitCommand command);
}
