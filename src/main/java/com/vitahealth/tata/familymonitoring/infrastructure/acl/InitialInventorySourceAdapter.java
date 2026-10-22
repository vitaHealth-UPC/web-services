package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.MedicationStock;
import com.vitahealth.tata.familymonitoring.domain.ports.InitialInventorySourcePort;
import com.vitahealth.tata.inventoryreplenishment.interfaces.acl.InventoryContextFacade;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Obtains source reservations through public contracts in medication-before-inventory order. */
@Component
public class InitialInventorySourceAdapter implements InitialInventorySourcePort {
  private final TreatmentContextFacade treatments;
  private final InventoryContextFacade inventories;

  /** Injects public owning-context boundaries rather than foreign repositories. */
  public InitialInventorySourceAdapter(
      TreatmentContextFacade treatments, InventoryContextFacade inventories) {
    this.treatments = treatments;
    this.inventories = inventories;
  }

  /** Delegates bounded adult medication discovery. */
  @Override
  public List<String> findMedicationIdsAfter(String olderAdultId, String afterId, int limit) {
    var source = treatments.findMedicationIdsAfter(olderAdultId, afterId, limit);
    try {
      var page = List.copyOf(source);
      if (page.size() > limit)
        throw new IllegalArgumentException("source medication page exceeds limit");
      var cursor = afterId;
      for (var id : page) {
        if (id.isBlank() || id.length() > 36 || id.compareTo(cursor) <= 0)
          throw new IllegalArgumentException("source medication page does not advance");
        cursor = id;
      }
      return page;
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new InvalidMonitoringSourceException(invalid);
    }
  }

  /** Preserves source ownership and source reservations for the caller projection transaction. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<MedicationStock> reserve(String olderAdultId, String medicationId) {
    var medication = treatments.reserveMedication(medicationId);
    if (medication.isEmpty()) return Optional.empty();
    var owner = medication.orElseThrow();
    if (!medicationId.equals(owner.id()) || !olderAdultId.equals(owner.olderAdultId()))
      throw new InvalidMonitoringSourceException(
          new IllegalStateException("medication source owner conflicts"));
    var stock =
        inventories
            .reserveStock(medicationId)
            .map(
                snapshot -> {
                  if (!medicationId.equals(snapshot.medicationId()))
                    throw new InvalidMonitoringSourceException(
                        new IllegalStateException("inventory medication conflicts"));
                  try {
                    return new InventoryStockPosition(
                        snapshot.medicationId(),
                        snapshot.inventoryId(),
                        snapshot.revision(),
                        snapshot.remainingStock(),
                        snapshot.replenishmentThreshold(),
                        snapshot.changedAt());
                  } catch (IllegalArgumentException | NullPointerException invalid) {
                    throw new InvalidMonitoringSourceException(invalid);
                  }
                });
    try {
      return Optional.of(new MedicationStock(owner.name(), stock));
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new InvalidMonitoringSourceException(invalid);
    }
  }
}
