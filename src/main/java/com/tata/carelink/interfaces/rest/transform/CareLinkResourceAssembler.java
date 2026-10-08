package com.tata.carelink.interfaces.rest.transform;

import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;
import com.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.tata.carelink.interfaces.rest.resources.*;

public final class CareLinkResourceAssembler {
    private CareLinkResourceAssembler() {}

    public static RegisterOlderAdultProfileCommand toCommand(RegisterOlderAdultProfileResource resource) {
        return new RegisterOlderAdultProfileCommand(
                resource.caregiverId(),
                resource.fullName(),
                resource.birthDate(),
                resource.emergencyContactName(),
                resource.emergencyContactRelationship(),
                resource.emergencyContactPhone()
        );
    }

    public static GenerateLinkingCodeCommand toCommand(GenerateLinkingCodeResource resource) {
        return new GenerateLinkingCodeCommand(resource.caregiverId(), resource.olderAdultId());
    }

    public static AcceptCareLinkCommand toCommand(AcceptCareLinkResource resource) {
        return new AcceptCareLinkCommand(resource.caregiverId(), resource.code());
    }

    public static RegisterConsentCommand toCommand(String careLinkId, RegisterConsentResource resource) {
        return new RegisterConsentCommand(careLinkId, resource.accepted());
    }

    public static OlderAdultProfileResource toResource(OlderAdultProfileResult result) {
        return new OlderAdultProfileResource(
                result.id(),
                result.registeredByCaregiverId(),
                result.fullName(),
                result.birthDate(),
                result.emergencyContactName(),
                result.emergencyContactRelationship(),
                result.emergencyContactPhone(),
                result.createdAt()
        );
    }

    public static CareLinkResource toResource(com.tata.carelink.application.models.AuthenticatedCareLinkResult result) {
        var l=result.link();
        return new CareLinkResource(l.id(),l.caregiverId(),l.olderAdultId(),l.status(),l.linkingCode(),l.codeExpiresAt(),l.codeUsedAt(),l.consentGranted(),l.consentRecordedAt(),l.confirmedAt(),result.accessToken(),result.expiresAt());
    }
    public static CareLinkResource toResource(CareLinkResult result) {
        return new CareLinkResource(
                result.id(),
                result.caregiverId(),
                result.olderAdultId(),
                result.status(),
                result.linkingCode(),
                result.codeExpiresAt(),
                result.codeUsedAt(),
                result.consentGranted(),
                result.consentRecordedAt(),
                result.confirmedAt()
        );
    }
}
