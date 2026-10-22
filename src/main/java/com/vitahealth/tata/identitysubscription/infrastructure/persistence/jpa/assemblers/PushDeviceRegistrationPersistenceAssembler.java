package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PushDeviceRegistrationPersistenceEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Maps owned device evidence and derives the storage uniqueness fingerprint. */
public final class PushDeviceRegistrationPersistenceAssembler {
  private PushDeviceRegistrationPersistenceAssembler() {}

  /** Restores exact stored evidence, rejecting corrupt rows as technical failures. */
  public static PushDeviceRegistration toDomain(PushDeviceRegistrationPersistenceEntity row) {
    try {
      return new PushDeviceRegistration(
          row.getInstallationId(),
          row.getSessionId(),
          row.getToken(),
          row.getCreatedAt(),
          row.getUpdatedAt());
    } catch (IllegalArgumentException | NullPointerException failure) {
      throw new IllegalStateException("stored push device is inconsistent");
    }
  }

  /** Copies a validated registration into a new persistence representation. */
  public static PushDeviceRegistrationPersistenceEntity toEntity(PushDeviceRegistration value) {
    return new PushDeviceRegistrationPersistenceEntity(
        value.installationId(),
        value.sessionId(),
        value.token(),
        fingerprint(value.token()),
        value.createdAt(),
        value.updatedAt());
  }

  /** Derives a unique lookup key without writing the opaque token into indexes. */
  public static String fingerprint(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }
}
