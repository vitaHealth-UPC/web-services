package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ReactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Delegates commands to handlers that own their individual transaction boundaries. */
@Service
public class TreatmentCommandServiceImpl implements TreatmentCommandService {
  private final ReactivateMedicationCommandHandler reactivations;
  private final RegisterMedicationCommandHandler registrations;
  private final CreateTreatmentCommandHandler creations;
  private final UpdateMedicationCommandHandler updates;
  private final DeactivateMedicationCommandHandler deactivations;
  private final ConfigureTreatmentCommandHandler configurations;
  private final ActivateTreatmentCommandHandler activations;
  private final PauseTreatmentCommandHandler pauses;
  private final ResumeTreatmentCommandHandler resumptions;

  /** Injects the medication and treatment command handlers. */
  public TreatmentCommandServiceImpl(
      RegisterMedicationCommandHandler registrations,
      CreateTreatmentCommandHandler creations,
      UpdateMedicationCommandHandler updates,
      DeactivateMedicationCommandHandler deactivations,
      ConfigureTreatmentCommandHandler configurations,
      ActivateTreatmentCommandHandler activations,
      PauseTreatmentCommandHandler pauses,
      ResumeTreatmentCommandHandler resumptions,
      ReactivateMedicationCommandHandler reactivations) {
    this.reactivations = reactivations;
    this.registrations = registrations;
    this.creations = creations;
    this.updates = updates;
    this.deactivations = deactivations;
    this.configurations = configurations;
    this.activations = activations;
    this.pauses = pauses;
    this.resumptions = resumptions;
  }

  /** Registers an active medication for an adult with current consented access. */
  @Override
  public Result<MedicationResult, ApplicationError> registerMedication(
      RegisterMedicationCommand command) {
    return execute(() -> registrations.handle(command));
  }

  /** Updates an active medication and republishes its dependent schedules atomically. */
  @Override
  public Result<MedicationResult, ApplicationError> updateMedication(
      UpdateMedicationCommand command) {
    return execute(() -> updates.handle(command));
  }

  /** Deactivates a medication and pauses its active dependent treatments atomically. */
  @Override
  public Result<MedicationResult, ApplicationError> deactivateMedication(
      DeactivateMedicationCommand command) {
    return execute(() -> deactivations.handle(command));
  }

  @Override
  public Result<MedicationResult, ApplicationError> reactivateMedication(
      ReactivateMedicationCommand command) {
    return execute(() -> reactivations.handle(command));
  }

  /** Creates an unconfigured draft for an adult with current consented access. */
  @Override
  public Result<TreatmentResult, ApplicationError> createTreatment(CreateTreatmentCommand command) {
    return execute(() -> creations.handle(command));
  }

  /** Replaces the regimen with a medication belonging to the same adult. */
  @Override
  public Result<TreatmentResult, ApplicationError> configureTreatment(
      ConfigureTreatmentCommand command) {
    return execute(() -> configurations.handle(command));
  }

  /** Activates a configured treatment whose medication remains active. */
  @Override
  public Result<TreatmentResult, ApplicationError> activate(ChangeTreatmentStatusCommand command) {
    return execute(() -> activations.handle(command));
  }

  /** Pauses an active treatment while preserving its regimen and history. */
  @Override
  public Result<TreatmentResult, ApplicationError> pause(ChangeTreatmentStatusCommand command) {
    return execute(() -> pauses.handle(command));
  }

  /** Resumes a paused treatment whose medication remains active. */
  @Override
  public Result<TreatmentResult, ApplicationError> resume(ChangeTreatmentStatusCommand command) {
    return execute(() -> resumptions.handle(command));
  }

  /** Captures expected rejections only after the handler transaction has completed rollback. */
  private <T> Result<T, ApplicationError> execute(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (TreatmentApplicationException failure) {
      return Result.failure(failure.toApplicationError());
    }
  }
}
