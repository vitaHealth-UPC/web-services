package com.vitahealth.tata.treatmentmanagement.interfaces.acl;

import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import java.util.List;
import java.util.Optional;

/**
 * Publishes caregiver-authorized reads and trusted integration evidence across context boundaries.
 */
public interface TreatmentContextFacade {
  /** Requires caregiver-authorized medication details. */
  MedicationResult getMedication(String caregiverId, String medicationId);

  /** Requires caregiver-authorized treatment details. */
  TreatmentResult getTreatment(String caregiverId, String treatmentId);

  /** Lists medication registrations within an authorized adult profile. */
  List<MedicationResult> listMedications(String caregiverId, String olderAdultId);

  /** Lists treatment regimens within an authorized adult profile. */
  List<TreatmentResult> listTreatments(String caregiverId, String olderAdultId);

  /**
   * Contract for other modules that react to events carrying only a medication id (low stock, for
   * example). It does no care-link check because there is no caregiver involved.
   */
  Optional<MedicationResult> findMedication(String medicationId);

  /** Reserves and refreshes medication evidence until the caller write transaction ends. */
  Optional<MedicationResult> reserveMedication(String medicationId);

  /** Reads trusted integration evidence without granting user-facing authorization. */
  Optional<TreatmentResult> findTreatment(String treatmentId);

  /**
   * Returns scheduled daily occurrences when supported, without inferring clinical dosage units.
   */
  Optional<Integer> scheduledDailyUnits(String medicationId);

  /**
   * Returns up to 100 adult-owned medication references after an exclusive cursor; empty starts.
   */
  List<String> findMedicationIdsAfter(String olderAdultId, String afterId, int limit);
}
