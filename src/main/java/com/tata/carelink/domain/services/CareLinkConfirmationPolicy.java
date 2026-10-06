package com.tata.carelink.domain.services;

import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;

import java.time.Instant;
import java.util.Objects;

public final class CareLinkConfirmationPolicy {
    public boolean canConfirm(CareLink careLink, Instant now) {
        Objects.requireNonNull(careLink);
        Objects.requireNonNull(now);
        return careLink.status() == CareLinkStatus.AWAITING_CONSENT
                && careLink.linkingCode().wasUsedWithinValidity()
                && careLink.consent().isGranted();
    }
}
