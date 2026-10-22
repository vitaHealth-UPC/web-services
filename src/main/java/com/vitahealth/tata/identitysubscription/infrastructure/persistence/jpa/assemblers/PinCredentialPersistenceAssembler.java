package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;

/** Converts PinCredential state between domain and JPA representations without database access. */
public final class PinCredentialPersistenceAssembler {
  private PinCredentialPersistenceAssembler() {}

  /**
   * Restores the stored identity and state without executing a business transition.
   *
   * @param entity stored JPA representation
   * @return reconstructed domain object
   */
  public static PinCredential toDomain(PinCredentialPersistenceEntity entity) {
    return PinCredential.rehydrate(
        entity.getId(),
        entity.getOlderAdultId(),
        entity.getPinHash(),
        entity.getFailedAttempts(),
        entity.getLockedUntil());
  }

  /**
   * Builds the persistence representation while preserving the domain identity.
   *
   * @param credential encoded credential and failure history owned by the domain
   * @return JPA state ready for the repository adapter
   */
  public static PinCredentialPersistenceEntity toEntity(PinCredential credential) {
    return new PinCredentialPersistenceEntity(
        credential.id(),
        credential.olderAdultId(),
        credential.pinHash(),
        credential.failedAttempts(),
        credential.lockedUntil());
  }
}
