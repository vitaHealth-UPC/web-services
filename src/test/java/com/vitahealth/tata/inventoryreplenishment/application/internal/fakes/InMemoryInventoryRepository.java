package com.vitahealth.tata.inventoryreplenishment.application.internal.fakes;

import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Stores rehydrated copies, like the JPA adapter, so tests cannot rely on shared instances. */
public final class InMemoryInventoryRepository implements InventoryRepository {
  private final Map<String, Inventory> byMedication = new HashMap<>();
  private final Map<String, String> consumptions = new HashMap<>();
  public int saveCalls;

  @Override
  public Inventory save(Inventory inventory) {
    saveCalls++;
    var existing = byMedication.get(inventory.medicationId());
    if (existing != null && !java.util.Objects.equals(existing.revision(), inventory.revision()))
      throw new IllegalStateException("obsolete inventory fixture snapshot");
    var stored = copy(inventory, existing == null ? 0L : Math.incrementExact(existing.revision()));
    byMedication.put(inventory.medicationId(), stored);
    return copy(stored);
  }

  @Override
  public Optional<Inventory> findByMedicationId(String medicationId) {
    return Optional.ofNullable(byMedication.get(medicationId))
        .map(InMemoryInventoryRepository::copy);
  }

  /** Unit-test lookup only; database reservation semantics require integration tests. */
  @Override
  public Optional<Inventory> findByMedicationIdForUpdate(String medicationId) {
    return findByMedicationId(medicationId);
  }

  @Override
  public boolean existsByMedicationId(String medicationId) {
    return byMedication.containsKey(medicationId);
  }

  @Override
  public boolean hasConsumed(String intakeId) {
    return consumptions.containsKey(intakeId);
  }

  @Override
  public void registerConsumption(String intakeId, String inventoryId, Instant consumedAt) {
    if (consumptions.putIfAbsent(intakeId, inventoryId) != null) {
      throw new IllegalStateException("intake already consumed");
    }
  }

  public int consumptionCount() {
    return consumptions.size();
  }

  private static Inventory copy(Inventory inventory) {
    return copy(inventory, inventory.revision());
  }

  private static Inventory copy(Inventory inventory, Long revision) {
    return Inventory.rehydrate(
        inventory.id(),
        inventory.medicationId(),
        inventory.remainingStock(),
        inventory.replenishmentThreshold(),
        inventory.batches(),
        inventory.createdAt(),
        inventory.updatedAt(),
        revision);
  }
}
