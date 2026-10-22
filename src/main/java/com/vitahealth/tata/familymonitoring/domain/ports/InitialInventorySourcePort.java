package com.vitahealth.tata.familymonitoring.domain.ports;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.MedicationStock;
import java.util.List;
import java.util.Optional;

/** Discovers adult-owned medication references and reserves their current inventory evidence. */
public interface InitialInventorySourcePort {
  /** Returns a bounded exclusive page without materializing all medications. */
  List<String> findMedicationIdsAfter(String olderAdultId, String afterId, int limit);

  /** Reserves medication before inventory and verifies actual adult ownership. */
  Optional<MedicationStock> reserve(String olderAdultId, String medicationId);
}
