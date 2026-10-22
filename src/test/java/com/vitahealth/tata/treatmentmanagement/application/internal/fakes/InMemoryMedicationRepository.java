package com.vitahealth.tata.treatmentmanagement.application.internal.fakes;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryMedicationRepository implements MedicationRepository {
  private final Map<String, Medication> values = new HashMap<>();

  @Override
  public Medication save(Medication medication) {
    values.put(medication.id(), medication);
    return medication;
  }

  @Override
  public Optional<Medication> findById(String id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public List<Medication> findByOlderAdultId(String olderAdultId) {
    return values.values().stream()
        .filter(medication -> medication.olderAdultId().equals(olderAdultId))
        .toList();
  }

  /** Implements the same bounded identity cursor for single-threaded application fixtures. */
  @Override
  public List<String> findIdsByOlderAdultIdAfter(String olderAdultId, String afterId, int limit) {
    if (limit < 1 || limit > 100)
      throw new IllegalArgumentException("invalid medication page limit");
    return values.values().stream()
        .filter(value -> value.olderAdultId().equals(olderAdultId))
        .map(Medication::id)
        .filter(id -> id.compareTo(afterId) > 0)
        .sorted()
        .limit(limit)
        .toList();
  }

  /** Single-threaded test substitute; database lock behavior is tested through JPA integration. */
  @Override
  public Optional<Medication> findForUpdate(String id) {
    return findById(id);
  }
}
