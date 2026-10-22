package com.vitahealth.tata.treatmentmanagement.application.commandservices;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;

/**
 * Public command outcomes; expected rejections are explicit values and technical failures
 * propagate.
 */
public interface TreatmentCommandService {
  /** Registers an active medication for an adult with current consented access. */
  Result<MedicationResult, ApplicationError> registerMedication(RegisterMedicationCommand command);

  /** Updates an active medication and republishes its dependent schedules atomically. */
  Result<MedicationResult, ApplicationError> updateMedication(UpdateMedicationCommand command);

  /** Deactivates a medication and pauses its active dependent treatments atomically. */
  Result<MedicationResult, ApplicationError> deactivateMedication(
      DeactivateMedicationCommand command);

  /** Creates an unconfigured draft for an adult with current consented access. */
  Result<TreatmentResult, ApplicationError> createTreatment(CreateTreatmentCommand command);

  /** Replaces the regimen with a medication belonging to the same adult. */
  Result<TreatmentResult, ApplicationError> configureTreatment(ConfigureTreatmentCommand command);

  /** Activates a configured treatment whose medication remains active. */
  Result<TreatmentResult, ApplicationError> activate(ChangeTreatmentStatusCommand command);

  /** Pauses an active treatment while preserving its regimen and history. */
  Result<TreatmentResult, ApplicationError> pause(ChangeTreatmentStatusCommand command);

  /** Resumes a paused treatment whose medication remains active. */
  Result<TreatmentResult, ApplicationError> resume(ChangeTreatmentStatusCommand command);
}
