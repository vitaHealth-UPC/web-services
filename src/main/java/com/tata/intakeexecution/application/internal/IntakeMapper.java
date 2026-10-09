package com.tata.intakeexecution.application.internal;

import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.model.aggregates.Intake;

public final class IntakeMapper {
    private IntakeMapper() {}

    public static IntakeResult toResult(Intake intake) {
        return toResult(intake, false);
    }

    /** Request metadata: true only when a confirmation command made no new transition. */
    public static IntakeResult toResult(Intake intake, boolean alreadyConfirmed) {
        return new IntakeResult(
                intake.id(),
                intake.treatmentId(),
                intake.medicationId(),
                intake.olderAdultId(),
                intake.medication().name(),
                intake.medication().dose(),
                intake.medication().instructions(),
                intake.scheduledAt(),
                intake.status(),
                intake.confirmedAt(),
                intake.confirmationChannel(),
                alreadyConfirmed
        );
    }
}
