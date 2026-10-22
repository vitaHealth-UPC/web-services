package com.vitahealth.tata.intakeexecution.interfaces.rest.transform;

import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.interfaces.rest.resources.ConfirmIntakeResource;

/**
 * Maps manual confirmation requests without trusting a client-claimed speech-validation channel.
 */
public final class ConfirmIntakeCommandFromResourceAssembler {
  private ConfirmIntakeCommandFromResourceAssembler() {}

  /** Accepts TOUCH only; VOICE evidence belongs to the validated recognition workflow. */
  public static ConfirmIntakeCommand toCommand(String intakeId, ConfirmIntakeResource resource) {
    if (!"TOUCH".equals(resource.channel()))
      throw new IllegalArgumentException(
          "spoken confirmation requires the voice recognition endpoint");
    return new ConfirmIntakeCommand(intakeId, ConfirmationChannel.TOUCH);
  }
}
