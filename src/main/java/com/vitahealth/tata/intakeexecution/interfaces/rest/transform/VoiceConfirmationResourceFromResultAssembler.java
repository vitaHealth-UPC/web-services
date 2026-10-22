package com.vitahealth.tata.intakeexecution.interfaces.rest.transform;

import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult;
import com.vitahealth.tata.intakeexecution.interfaces.rest.resources.VoiceConfirmationResource;

/** Maps recognition outcomes and optional intake evidence into the voice HTTP representation. */
public final class VoiceConfirmationResourceFromResultAssembler {
  private VoiceConfirmationResourceFromResultAssembler() {}

  /** Preserves null intake evidence when recognition did not confirm a transition. */
  public static VoiceConfirmationResource toResource(VoiceConfirmationResult result) {
    return new VoiceConfirmationResource(
        result.status(),
        result.transcript(),
        result.confidence(),
        result.intake() == null
            ? null
            : IntakeResourceFromResultAssembler.toResource(result.intake()));
  }
}
