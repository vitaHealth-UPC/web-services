package com.tata.carelink.application.internal;

import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;
import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;

public final class CareLinkMapper {
    private CareLinkMapper() {}

    public static CareLinkResult toResult(CareLink link) {
        return new CareLinkResult(
                link.id(),
                link.caregiverId(),
                link.olderAdultId(),
                link.status(),
                link.linkingCode().value(),
                link.linkingCode().expiresAt(),
                link.linkingCode().usedAt(),
                link.consent().isGranted(),
                link.consent().recordedAt(),
                link.confirmedAt()
        );
    }

    public static OlderAdultProfileResult toResult(OlderAdultProfile profile) {
        var contact = profile.emergencyContact();
        return new OlderAdultProfileResult(
                profile.id(),
                profile.registeredByCaregiverId(),
                profile.basicData().fullName(),
                profile.basicData().birthDate(),
                contact == null ? null : contact.name(),
                contact == null ? null : contact.relationship(),
                contact == null ? null : contact.phone(),
                profile.createdAt()
        );
    }
}
