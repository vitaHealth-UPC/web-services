package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** Stores coordination keys independently of whether a user's preferences have been created. */
@Entity
@Table(
    name = "user_preference_locks",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_preferences_mutation_user", columnNames = "user_id"))
public class PreferencesMutationLockEntity {
  @Id
  @Column(nullable = false, updatable = false, length = 36)
  private String id;

  @Column(name = "user_id", nullable = false, updatable = false, length = 64)
  private String userId;

  /** Required by JPA. */
  protected PreferencesMutationLockEntity() {}

  /** Creates a technical key without saving default preferences or a foreign account reference. */
  public PreferencesMutationLockEntity(String userId) {
    this.id = UUID.randomUUID().toString();
    this.userId = userId;
  }
}
