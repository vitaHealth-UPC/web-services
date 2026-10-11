package com.tata.omissionescalation.application.internal.outboundservices;

/** Coordinates the omission decision with the source intake transaction. */
public interface IntakeOmissionPort {
    /** Holds the intake lock until the surrounding transaction completes. */
    boolean lockPendingIntake(String intakeId);
    /** Locks and verifies the confirmation recorded by Intake Execution before closing a case. */
    boolean lockRecordedConfirmation(String intakeId, java.time.Instant confirmedAt);
}
