package com.tata.omissionescalation.infrastructure.persistence.jpa.embeddables;
import jakarta.persistence.*;
import java.time.Instant;
@Embeddable
public record GracePeriodPersistenceEmbeddable(@Column(name="grace_started_at",nullable=false) Instant startsAt, @Column(name="grace_ends_at",nullable=false) Instant endsAt) {}
