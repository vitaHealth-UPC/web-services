package com.vitahealth.tata.omissionescalation.infrastructure.acl;

import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Translates the source intake facade into the omission context contract. */
@Component
public class IntakeOmissionAdapter implements IntakeOmissionPort {
  private final IntakeContextFacade intakes;

  /** Injects the public intake boundary; source reservations remain owned by Intake Execution. */
  public IntakeOmissionAdapter(IntakeContextFacade intakes) {
    this.intakes = intakes;
  }

  /** Reserves only pending intake evidence in the caller transaction before omission mutation. */
  @Override
  public boolean lockPendingIntake(String intakeId) {
    return intakes.lockPendingIntake(intakeId);
  }

  /** Rechecks the exact recorded confirmation before resolving its associated pending case. */
  @Override
  public boolean lockRecordedConfirmation(String intakeId, Instant confirmedAt) {
    return intakes.lockRecordedConfirmation(intakeId, confirmedAt);
  }
}
