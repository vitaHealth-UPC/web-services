package com.vitahealth.tata.treatmentmanagement.application.internal;

import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;

/** Projects treatment and medication aggregates without persistence access. */
public final class TreatmentMapper {
  private TreatmentMapper() {}

  /** Maps medication identity and current registration data. */
  public static MedicationResult toResult(Medication medication) {
    return new MedicationResult(
        medication.id(),
        medication.olderAdultId(),
        medication.name(),
        medication.presentation(),
        medication.active());
  }

  /** Maps the configured regimen including its explicit recurrence. */
  public static TreatmentResult toResult(Treatment treatment) {
    var regimen = treatment.regimen();
    return new TreatmentResult(
        treatment.id(),
        treatment.olderAdultId(),
        treatment.name(),
        treatment.status(),
        regimen == null ? null : regimen.medicationId(),
        regimen == null ? null : regimen.dose(),
        regimen == null ? null : regimen.frequency(),
        regimen == null ? null : regimen.scheduledTimes(),
        regimen == null ? null : regimen.instructions(),
        regimen == null ? null : regimen.reminderLeadMinutes(),
        regimen == null ? null : regimen.recurrence().mode().name(),
        regimen == null ? null : regimen.recurrence().zone(),
        regimen == null ? null : regimen.recurrence().anchor());
  }
}
