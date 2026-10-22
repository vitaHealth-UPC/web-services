package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reads context-owned evidence for trusted integration consumers within one read transaction. */
@Service
@Transactional(readOnly = true)
public class TreatmentIntegrationQueryService {
  private final MedicationRepository medications;
  private final TreatmentRepository treatments;

  /** Injects only repositories owned by Treatment Management. */
  public TreatmentIntegrationQueryService(
      MedicationRepository medications, TreatmentRepository treatments) {
    this.medications = medications;
    this.treatments = treatments;
  }

  /**
   * Resolves medication evidence without a caregiver because integration events carry resource
   * identity.
   */
  public Optional<MedicationResult> findMedication(String medicationId) {
    return medications.findById(medicationId).map(TreatmentMapper::toResult);
  }

  /** Reserves current medication evidence for a caller coordinating cross-context stock work. */
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<MedicationResult> reserveMedication(String medicationId) {
    return medications
        .findForUpdate(requireText(medicationId, "medicationId"))
        .map(TreatmentMapper::toResult);
  }

  /** Reads a bounded page of actual adult-owned references for resumable integration work. */
  public List<String> findMedicationIdsAfter(String olderAdultId, String afterId, int limit) {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    Objects.requireNonNull(afterId, "afterId");
    if (limit < 1 || limit > 100)
      throw new IllegalArgumentException("medication page limit must be between 1 and 100");
    return List.copyOf(medications.findIdsByOlderAdultIdAfter(olderAdultId, afterId, limit));
  }

  /** Resolves treatment evidence without manufacturing a missing aggregate. */
  public Optional<TreatmentResult> findTreatment(String treatmentId) {
    return treatments.findById(treatmentId).map(TreatmentMapper::toResult);
  }

  /** Counts active scheduled occurrences; this count does not parse dose text into stock units. */
  public Optional<Integer> scheduledDailyOccurrences(String medicationId) {
    return medications
        .findById(medicationId)
        .filter(m -> m.active())
        .flatMap(
            m -> {
              int count =
                  treatments.findByMedicationId(medicationId).stream()
                      .filter(t -> t.olderAdultId().equals(m.olderAdultId()))
                      .filter(t -> t.status() == TreatmentStatus.ACTIVE)
                      .filter(
                          t ->
                              t.regimen() != null
                                  && medicationId.equals(t.regimen().medicationId()))
                      .mapToInt(t -> t.regimen().scheduledTimes().size())
                      .sum();
              return count > 0 ? Optional.of(count) : Optional.empty();
            });
  }
}
