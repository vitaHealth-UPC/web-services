package com.tata.treatmentmanagement.application.internal.outboundservices.acl;

import com.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.tata.treatmentmanagement.domain.services.ICareLinkVerificationPort;
import org.springframework.stereotype.Component;

@Component
public class CareLinkVerificationAdapter implements ICareLinkVerificationPort {
    private final CareLinkContextFacade careLinkQueryService;

    public CareLinkVerificationAdapter(CareLinkContextFacade careLinkQueryService) {
        this.careLinkQueryService = careLinkQueryService;
    }

    @Override
    public boolean isAuthorized(String caregiverId, String olderAdultId) {
        return careLinkQueryService.isAuthorized(caregiverId, olderAdultId);
    }
}
