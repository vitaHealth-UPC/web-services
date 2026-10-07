package com.tata.omissionescalation.infrastructure.persistence.jpa.embeddables;
import jakarta.persistence.*;
@Embeddable
public record EscalationLevelPersistenceEmbeddable(@Column(name="level",nullable=false) int value) {}
