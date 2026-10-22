package com.vitahealth.tata.inventoryreplenishment.interfaces.acl;

import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryStockSnapshot;
import java.util.Optional;

/** Public inventory evidence boundary for transactional projections in other contexts. */
public interface InventoryContextFacade {
  /** Reserves and refreshes existing stock until the caller write transaction ends. */
  Optional<InventoryStockSnapshot> reserveStock(String medicationId);
}
