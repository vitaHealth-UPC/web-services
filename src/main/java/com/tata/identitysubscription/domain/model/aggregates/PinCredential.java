package com.tata.identitysubscription.domain.model.aggregates;

import static com.tata.shared.domain.validation.DomainText.requireText;

import com.tata.identitysubscription.domain.model.valueobjects.PinPolicy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PinCredential {
    private final String id;
    private final String olderAdultId;
    private final String pinHash;
    private int failedAttempts;
    private Instant lockedUntil;

    private PinCredential(
            String id,
            String olderAdultId,
            String pinHash,
            int failedAttempts,
            Instant lockedUntil
    ) {
        this.id = Objects.requireNonNull(id);
        this.olderAdultId = requireText(olderAdultId, "olderAdultId");
        this.pinHash = requireText(pinHash, "pinHash");
        this.failedAttempts = Math.max(0, failedAttempts);
        this.lockedUntil = lockedUntil;
    }

    public static PinCredential register(String olderAdultId, String pinHash) {
        return new PinCredential(UUID.randomUUID().toString(), olderAdultId, pinHash, 0, null);
    }

    public static PinCredential rehydrate(
            String id,
            String olderAdultId,
            String pinHash,
            int failedAttempts,
            Instant lockedUntil
    ) {
        return new PinCredential(id, olderAdultId, pinHash, failedAttempts, lockedUntil);
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void registerFailure(Instant now, PinPolicy policy) {
        if (isLockedAt(now)) return;
        failedAttempts++;
        if (failedAttempts >= policy.maxFailedAttempts()) {
            lockedUntil = now.plus(policy.lockDuration());
        }
    }

    public void registerSuccess() {
        failedAttempts = 0;
        lockedUntil = null;
    }


    public String id() { return id; }
    public String olderAdultId() { return olderAdultId; }
    public String pinHash() { return pinHash; }
    public int failedAttempts() { return failedAttempts; }
    public Instant lockedUntil() { return lockedUntil; }
}
