package com.tata.identitysubscription.domain.model.valueobjects;

import java.time.Duration;

public record PinPolicy(int maxFailedAttempts, Duration lockDuration) {
    public PinPolicy {
        if (maxFailedAttempts < 1) throw new IllegalArgumentException("maxFailedAttempts must be positive");
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("lockDuration must be positive");
        }
    }

    public static PinPolicy defaultPolicy() {
        return new PinPolicy(4, Duration.ofMinutes(15));
    }
}
