package com.tata.omissionescalation.application.internal.outboundservices;

/** Coordinates the omission decision with the source intake transaction. */
public interface IntakeOmissionPort {
    /** Holds the intake lock until the surrounding transaction completes. */
    boolean lockPendingIntake(String intakeId);
}
