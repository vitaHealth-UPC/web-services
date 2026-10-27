package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PasswordRecoveryChallenge;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PasswordRecoveryPersistenceEntity;

public final class PasswordRecoveryPersistenceAssembler {
  private PasswordRecoveryPersistenceAssembler() {}

  public static PasswordRecoveryChallenge toDomain(PasswordRecoveryPersistenceEntity value) {
    return new PasswordRecoveryChallenge(
        value.getCodeHash(),
        value.getIssuedAt(),
        value.getExpiresAt(),
        value.getFailedAttempts(),
        value.getConsumedAt());
  }

  public static PasswordRecoveryPersistenceEntity toEntity(
      String accountId, PasswordRecoveryChallenge value) {
    return new PasswordRecoveryPersistenceEntity(
        accountId,
        value.codeHash(),
        value.issuedAt(),
        value.expiresAt(),
        value.failedAttempts(),
        value.consumedAt());
  }
}
