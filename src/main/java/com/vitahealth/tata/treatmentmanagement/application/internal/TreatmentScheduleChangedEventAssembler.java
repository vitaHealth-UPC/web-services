package com.vitahealth.tata.treatmentmanagement.application.internal;

import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.util.Objects;

/** Builds the same schedule snapshot for command changes and horizon extension. */
public final class TreatmentScheduleChangedEventAssembler {
  private TreatmentScheduleChangedEventAssembler() {}

  /** Maps consistent aggregate evidence without loading repositories or publishing events. */
  public static TreatmentScheduleChangedEvent toEvent(Treatment treatment, Medication medication) {
    Objects.requireNonNull(treatment, "treatment");
    Objects.requireNonNull(medication, "medication");
    var regimen = Objects.requireNonNull(treatment.regimen(), "configured regimen");
    if (!regimen.medicationId().equals(medication.id())
        || !treatment.olderAdultId().equals(medication.olderAdultId())) {
      throw new IllegalArgumentException("schedule medication must belong to its treatment owner");
    }
    return new TreatmentScheduleChangedEvent(
        treatment.id(),
        regimen.medicationId(),
        treatment.olderAdultId(),
        medication.name(),
        regimen.dose(),
        regimen.frequency(),
        regimen.scheduledTimes(),
        regimen.instructions(),
        regimen.reminderLeadMinutes(),
        treatment.status() == TreatmentStatus.ACTIVE,
        regimen.recurrence().mode().name(),
        regimen.recurrence().zone(),
        regimen.recurrence().anchor());
  }
}
