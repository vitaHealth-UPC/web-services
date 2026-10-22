package com.vitahealth.tata.intakeexecution.application.commandservices;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.GenerateIntakesCommand;
import java.util.List;

/** Applies a treatment schedule snapshot without exposing the reconciliation implementation. */
public interface GenerateIntakesCommandService {
  /** Reconciles pending slots and retains definitive outcomes in the publishing transaction. */
  List<IntakeResult> handle(GenerateIntakesCommand command);
}
