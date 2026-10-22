package com.vitahealth.tata.inventoryreplenishment.application.internal.queryservices;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryStockSnapshot;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reserves complete source evidence without mutating stock or publishing business events. */
@Service
public class InventoryIntegrationQueryService {
  private final InventoryRepository inventories;

  /** Injects only context-owned storage. */
  public InventoryIntegrationQueryService(InventoryRepository inventories) {
    this.inventories = inventories;
  }

  /** Preserves the caller's source-before-projection reservation boundary. */
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<InventoryStockSnapshot> reserveStock(String medicationId) {
    return inventories
        .findByMedicationIdForUpdate(requireText(medicationId, "medicationId"))
        .map(
            stock ->
                new InventoryStockSnapshot(
                    stock.id(),
                    stock.medicationId(),
                    stock.remainingStock(),
                    stock.replenishmentThreshold(),
                    stock.revision(),
                    stock.updatedAt()));
  }
}
