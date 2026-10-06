package com.tata.omissionescalation.infrastructure.modules;

import com.tata.intakeexecution.interfaces.acl.IntakeOmissionFacade;
import com.tata.omissionescalation.application.internal.outboundservices.IIntakeOmissionPort;
import org.springframework.stereotype.Component;

@Component
public class IntakeOmissionAdapter implements IIntakeOmissionPort {
    private final IntakeOmissionFacade facade;
    public IntakeOmissionAdapter(IntakeOmissionFacade facade) { this.facade = facade; }
    public boolean lockPendingIntake(String intakeId) { return facade.lockPendingIntake(intakeId); }
}
