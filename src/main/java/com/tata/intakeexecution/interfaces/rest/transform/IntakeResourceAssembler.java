package com.tata.intakeexecution.interfaces.rest.transform;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult;
import com.tata.intakeexecution.interfaces.rest.resources.IntakeResource;
import com.tata.intakeexecution.interfaces.rest.resources.VoiceConfirmationResource;
public final class IntakeResourceAssembler {
    private IntakeResourceAssembler() {}
    public static VoiceConfirmationResource toResource(VoiceConfirmationResult result) {
        return new VoiceConfirmationResource(
                result.status(),
                result.transcript(),
                result.confidence(),
                result.intake() == null ? null : toResource(result.intake())
        );
    }

    public static IntakeResource toResource(IntakeResult result) {
        return new IntakeResource(
                result.id(),
                result.treatmentId(),
                result.medicationId(),
                result.olderAdultId(),
                result.medicationName(),
                result.dose(),
                result.instructions(),
                result.scheduledAt(),
                result.status(),
                result.confirmedAt(),
                result.confirmationChannel(),
                result.alreadyConfirmed()
        );
    }
}
