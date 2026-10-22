package com.vitahealth.tata.intakeexecution.domain.model.commands;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;

/** Records a confirmation using the channel established by the invoking application flow. */
public record ConfirmIntakeCommand(String intakeId, ConfirmationChannel channel) {
  /** Requires an explicit evidence reference and confirmation channel. */
  public ConfirmIntakeCommand {
    if (intakeId == null || intakeId.isBlank()) {
      throw new IllegalArgumentException("intakeId is required");
    }
    if (channel == null) {
      throw new IllegalArgumentException("confirmation channel is required");
    }

    intakeId = intakeId.trim();
  }
}
