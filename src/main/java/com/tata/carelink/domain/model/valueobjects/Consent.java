package com.tata.carelink.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

public final class Consent {
    private final boolean accepted;
    private final Instant recordedAt;

    private Consent(boolean accepted, Instant recordedAt) {
        this.accepted = accepted;
        this.recordedAt = recordedAt;
    }

    public static Consent pending() {
        return new Consent(false, null);
    }

    public static Consent record(boolean accepted, Instant now) {
        return new Consent(accepted, Objects.requireNonNull(now));
    }

    public static Consent rehydrate(boolean accepted, Instant recordedAt) {
        return new Consent(accepted, recordedAt);
    }

    public boolean isGranted() {
        return accepted && recordedAt != null;
    }

    public boolean accepted() { return accepted; }
    public Instant recordedAt() { return recordedAt; }
}
