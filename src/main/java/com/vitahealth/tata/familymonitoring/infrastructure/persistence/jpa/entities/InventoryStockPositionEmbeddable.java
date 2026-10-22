package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Stores owned complete source evidence without an independent business identity. */
@Embeddable
public class InventoryStockPositionEmbeddable {
  @Column(name = "medication_id", nullable = false, length = 36)
  private String medicationId;

  @Column(name = "inventory_id", nullable = false, length = 36)
  private String inventoryId;

  @Column(name = "source_revision", nullable = false)
  private long revision;

  @Column(name = "remaining_stock", nullable = false)
  private int remainingStock;

  @Column(name = "replenishment_threshold", nullable = false)
  private int replenishmentThreshold;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** Required by JPA field-access restoration. */
  protected InventoryStockPositionEmbeddable() {}

  /** Copies validated complete source evidence into its storage representation. */
  public InventoryStockPositionEmbeddable(InventoryStockPosition position) {
    medicationId = position.medicationId();
    inventoryId = position.inventoryId();
    revision = position.revision();
    remainingStock = position.remainingStock();
    replenishmentThreshold = position.replenishmentThreshold();
    changedAt = position.changedAt();
  }

  /** Restores and validates immutable domain evidence from stored columns. */
  public InventoryStockPosition toDomain() {
    return new InventoryStockPosition(
        medicationId, inventoryId, revision, remainingStock, replenishmentThreshold, changedAt);
  }
}
