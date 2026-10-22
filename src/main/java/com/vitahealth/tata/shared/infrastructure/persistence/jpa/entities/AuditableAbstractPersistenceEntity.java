package com.vitahealth.tata.shared.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Common technical persistence audit, adapted from learning-center-platform under MIT.
 *
 * <p>Each context retains its existing identity. These timestamps describe database writes and must
 * not replace clinical occurrence times maintained by domain aggregates.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableAbstractPersistenceEntity {
  @CreatedDate
  @Column(name = "persisted_at", nullable = false, updatable = false)
  private Instant persistedAt;

  @LastModifiedDate
  @Column(name = "modified_at", nullable = false)
  private Instant modifiedAt;

  /** Constructor for context persistence entities and JPA. */
  protected AuditableAbstractPersistenceEntity() {}

  /** Returns the first database persistence time, independent of domain event time. */
  public Instant getPersistedAt() {
    return persistedAt;
  }

  /** Returns the most recent database modification time. */
  public Instant getModifiedAt() {
    return modifiedAt;
  }
}
