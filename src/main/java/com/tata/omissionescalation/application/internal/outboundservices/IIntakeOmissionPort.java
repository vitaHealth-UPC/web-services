package com.tata.omissionescalation.application.internal.outboundservices;

public interface IIntakeOmissionPort {
    /** Must run inside the omission transaction; holds the source intake lock until completion. */
    boolean lockPendingIntake(String intakeId);
}
