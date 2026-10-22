package com.vitahealth.tata.treatmentmanagement.domain.repositories;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import java.util.List;
import java.util.Optional;

/** Persists medication aggregate evidence independently of its storage technology. */
public interface MedicationRepository {
  Medication save(Medication medication);

  Optional<Medication> findById(String id);

  /** Loads current evidence with an exclusive write lock retained by the caller transaction. */
  Optional<Medication> findForUpdate(String id);

  List<Medication> findByOlderAdultId(String olderAdultId);

  /** Returns at most limit source references in identity order, strictly after the cursor. */
  List<String> findIdsByOlderAdultIdAfter(String olderAdultId, String afterId, int limit);
}
