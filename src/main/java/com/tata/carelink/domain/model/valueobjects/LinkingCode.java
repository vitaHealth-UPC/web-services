package com.tata.carelink.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class LinkingCode {
    private final String value;
    private final Instant expiresAt;
    private Instant usedAt;

    private LinkingCode(String value, Instant expiresAt, Instant usedAt) {
        this.value = requireText(value);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.usedAt = usedAt;
    }

    public static LinkingCode issue(String value, Instant now, Duration ttl) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(ttl);
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("linking code ttl must be positive");
        }
        return new LinkingCode(value, now.plus(ttl), null);
    }

    public static LinkingCode rehydrate(String value, Instant expiresAt, Instant usedAt) {
        return new LinkingCode(value, expiresAt, usedAt);
    }

    public boolean isValid(Instant now) {
        Objects.requireNonNull(now);
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        if (!isValid(now)) {
            throw new IllegalStateException("linking code is expired or already used");
        }
        usedAt = now;
    }

    public boolean wasUsedWithinValidity() {
        return usedAt != null && !usedAt.isAfter(expiresAt);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("linking code is required");
        }
        return value.trim().toUpperCase();
    }

    public String value() { return value; }
    public Instant expiresAt() { return expiresAt; }
    public Instant usedAt() { return usedAt; }
}
