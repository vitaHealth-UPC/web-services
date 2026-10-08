package com.tata.omissionescalation.application.internal.outboundservices.acl;

import com.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import com.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import org.springframework.stereotype.Service;

/** Translates the source intake facade into the omission context contract. */
@Service
public class IntakeOmissionAdapter implements IntakeOmissionPort {
    private final IntakeContextFacade intakes;
    public IntakeOmissionAdapter(IntakeContextFacade intakes) { this.intakes = intakes; }
    @Override public boolean lockPendingIntake(String intakeId) { return intakes.lockPendingIntake(intakeId); }
}
