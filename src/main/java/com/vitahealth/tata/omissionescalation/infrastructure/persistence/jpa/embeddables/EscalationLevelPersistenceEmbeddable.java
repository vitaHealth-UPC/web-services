package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Stores the numeric attention level; domain reconstruction validates its business range. */
@Embeddable
public record EscalationLevelPersistenceEmbeddable(
    @Column(name = "level", nullable = false) int value) {}
