package com.tata.carelink.domain.model.aggregates;

import static com.tata.shared.domain.validation.DomainText.requireText;

import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.tata.carelink.domain.model.valueobjects.Consent;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class CareLink {
    private final String id;
    private final String caregiverId;
    private final String olderAdultId;
    private CareLinkStatus status;
    private final LinkingCode linkingCode;
    private Consent consent;
    private Instant confirmedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private CareLink(
            String id,
            String caregiverId,
            String olderAdultId,
            CareLinkStatus status,
            LinkingCode linkingCode,
            Consent consent,
            Instant confirmedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireText(id, "id");
        this.caregiverId = requireText(caregiverId, "caregiverId");
        this.olderAdultId = requireText(olderAdultId, "olderAdultId");
        this.status = Objects.requireNonNull(status);
        this.linkingCode = Objects.requireNonNull(linkingCode);
        this.consent = Objects.requireNonNull(consent);
        this.confirmedAt = confirmedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static CareLink createPending(
            String caregiverId,
            String olderAdultId,
            LinkingCode linkingCode,
            Instant now
    ) {
        Objects.requireNonNull(now);
        return new CareLink(
                UUID.randomUUID().toString(),
                caregiverId,
                olderAdultId,
                CareLinkStatus.PENDING,
                linkingCode,
                Consent.pending(),
                null,
                now,
                now
        );
    }

    public static CareLink rehydrate(
            String id,
            String caregiverId,
            String olderAdultId,
            CareLinkStatus status,
            LinkingCode linkingCode,
            Consent consent,
            Instant confirmedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new CareLink(id, caregiverId, olderAdultId, status, linkingCode, consent, confirmedAt, createdAt, updatedAt);
    }

    public void accept(String suppliedCode, Instant now) {
        if (status != CareLinkStatus.PENDING) {
            throw new IllegalStateException("care link is not pending");
        }
        if (suppliedCode == null || !linkingCode.value().equalsIgnoreCase(suppliedCode.trim())) {
            throw new IllegalArgumentException("linking code is invalid");
        }
        linkingCode.markUsed(now);
        status = CareLinkStatus.AWAITING_CONSENT;
        updatedAt = now;
    }

    public void registerConsent(boolean accepted, Instant now) {
        if (status != CareLinkStatus.AWAITING_CONSENT) {
            throw new IllegalStateException("care link is not awaiting consent");
        }
        consent = Consent.record(accepted, now);
        updatedAt = now;
        if (!accepted) {
            status = CareLinkStatus.REVOKED;
        }
    }

    public void confirm(Instant now) {
        if (status != CareLinkStatus.AWAITING_CONSENT) {
            throw new IllegalStateException("care link is not awaiting consent");
        }
        if (!consent.isGranted()) {
            throw new IllegalStateException("consent is required");
        }
        status = CareLinkStatus.CONFIRMED;
        confirmedAt = Objects.requireNonNull(now);
        updatedAt = now;
    }

    public boolean isActive() {
        return status == CareLinkStatus.CONFIRMED && consent.isGranted();
    }


    public String id() { return id; }
    public String caregiverId() { return caregiverId; }
    public String olderAdultId() { return olderAdultId; }
    public CareLinkStatus status() { return status; }
    public LinkingCode linkingCode() { return linkingCode; }
    public Consent consent() { return consent; }
    public Instant confirmedAt() { return confirmedAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
