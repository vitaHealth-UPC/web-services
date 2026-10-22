package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Stores both grace boundaries; domain reconstruction validates their chronological order. */
@Embeddable
public record GracePeriodPersistenceEmbeddable(
    @Column(name = "grace_started_at", nullable = false) Instant startsAt,
    @Column(name = "grace_ends_at", nullable = false) Instant endsAt) {}
