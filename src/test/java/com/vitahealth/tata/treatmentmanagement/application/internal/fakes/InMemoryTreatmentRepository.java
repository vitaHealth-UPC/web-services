package com.vitahealth.tata.treatmentmanagement.application.internal.fakes;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryTreatmentRepository implements TreatmentRepository {
  private final Map<String, Treatment> values = new HashMap<>();

  @Override
  public Treatment save(Treatment treatment) {
    values.put(treatment.id(), treatment);
    return treatment;
  }

  @Override
  public Optional<Treatment> findById(String id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public List<Treatment> findByOlderAdultId(String olderAdultId) {
    return values.values().stream()
        .filter(treatment -> treatment.olderAdultId().equals(olderAdultId))
        .toList();
  }

  @Override
  public List<Treatment> findByMedicationId(String medicationId) {
    return values.values().stream()
        .filter(
            treatment ->
                treatment.regimen() != null
                    && treatment.regimen().medicationId().equals(medicationId))
        .toList();
  }

  @Override
  public List<Treatment> findByStatus(TreatmentStatus status) {
    return values.values().stream().filter(treatment -> treatment.status() == status).toList();
  }

  /** Single-threaded test substitute; database lock behavior is tested through JPA integration. */
  @Override
  public Optional<Treatment> findForUpdate(String id) {
    return findById(id);
  }

  /** Mirrors ordered cursor reads for single-threaded handler tests. */
  @Override
  public List<String> findIdsByStatusAfter(TreatmentStatus status, String afterId, int limit) {
    return values.values().stream()
        .filter(t -> t.status() == status)
        .map(Treatment::id)
        .filter(id -> id.compareTo(afterId) > 0)
        .sorted()
        .limit(limit)
        .toList();
  }
}
