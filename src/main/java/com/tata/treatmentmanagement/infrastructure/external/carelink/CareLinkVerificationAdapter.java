package com.tata.treatmentmanagement.infrastructure.external.carelink;

import com.tata.carelink.application.queryservices.CareLinkQueryService;
import com.tata.treatmentmanagement.domain.services.ICareLinkVerificationPort;
import org.springframework.stereotype.Component;

@Component
public class CareLinkVerificationAdapter implements ICareLinkVerificationPort {
    private final CareLinkQueryService careLinkQueryService;

    public CareLinkVerificationAdapter(CareLinkQueryService careLinkQueryService) {
        this.careLinkQueryService = careLinkQueryService;
    }

    @Override
    public boolean isAuthorized(String caregiverId, String olderAdultId) {
        return careLinkQueryService.isAuthorized(caregiverId, olderAdultId);
    }
}
