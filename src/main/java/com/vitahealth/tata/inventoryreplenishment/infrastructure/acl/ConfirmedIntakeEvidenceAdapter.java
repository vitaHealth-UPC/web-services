package com.vitahealth.tata.inventoryreplenishment.infrastructure.acl;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.ConfirmedIntakeEvidence;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Translates the public Intake confirmation contract without accessing its persistence layer. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ConfirmedIntakeEvidenceAdapter implements ConfirmedIntakeEvidence {
  private final IntakeContextFacade intakes;

  /** Injects the source context public evidence and reservation boundary. */
  public ConfirmedIntakeEvidenceAdapter(IntakeContextFacade intakes) {
    this.intakes = intakes;
  }

  /** Checks every source reference after obtaining the definitive confirmation reservation. */
  @Override
  public boolean lockAndVerify(RequestInventoryConsumptionCommand command) {
    Objects.requireNonNull(command, "command");
    if (!intakes.lockRecordedConfirmation(command.intakeId(), command.confirmedAt())) return false;
    return intakes
        .findIntake(command.intakeId())
        .filter(
            source ->
                command.intakeId().equals(source.id())
                    && command.medicationId().equals(source.medicationId())
                    && command.olderAdultId().equals(source.olderAdultId())
                    && command.confirmedAt().equals(source.confirmedAt())
                    && (source.status() == IntakeStatus.CONFIRMED
                        || source.status() == IntakeStatus.LATE))
        .isPresent();
  }
}
