package com.vitahealth.tata.treatmentmanagement.domain.repositories;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.util.List;
import java.util.Optional;

/** Persists treatment aggregate evidence independently of its storage technology. */
public interface TreatmentRepository {
  Treatment save(Treatment treatment);

  Optional<Treatment> findById(String id);

  /** Loads current evidence with an exclusive write lock retained by the caller transaction. */
  Optional<Treatment> findForUpdate(String id);

  List<Treatment> findByOlderAdultId(String olderAdultId);

  List<Treatment> findByMedicationId(String medicationId);

  List<Treatment> findByStatus(TreatmentStatus status);

  /**
   * Returns at most limit ordered IDs strictly after the cursor, without loading aggregate
   * regimens.
   */
  List<String> findIdsByStatusAfter(TreatmentStatus status, String afterId, int limit);
}
