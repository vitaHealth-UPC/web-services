package com.vitahealth.tata.inventoryreplenishment.application.internal.acl;

import com.vitahealth.tata.inventoryreplenishment.application.internal.queryservices.InventoryIntegrationQueryService;
import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryStockSnapshot;
import com.vitahealth.tata.inventoryreplenishment.interfaces.acl.InventoryContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Delegates integration reads to the inventory application transaction boundary. */
@Service
public class InventoryContextFacadeImpl implements InventoryContextFacade {
  private final InventoryIntegrationQueryService queries;

  /** Injects the source reservation query service. */
  public InventoryContextFacadeImpl(InventoryIntegrationQueryService queries) {
    this.queries = queries;
  }

  /** Returns current reserved evidence without exposing context-owned storage. */
  @Override
  public Optional<InventoryStockSnapshot> reserveStock(String medicationId) {
    return queries.reserveStock(medicationId);
  }
}
