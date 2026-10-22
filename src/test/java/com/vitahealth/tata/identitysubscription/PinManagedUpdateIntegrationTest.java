package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinPolicy;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PinCredentialJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifies managed PIN updates preserve ownership and persist authentication history. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PinManagedUpdateIntegrationTest {
  @Autowired private PinCredentialRepository credentials;
  @Autowired private PinCredentialJpaRepository rows;

  @Test
  void failureAndSuccessUpdateTheSameManagedCredential() {
    var credential =
        credentials.save(PinCredential.register(UUID.randomUUID().toString(), "encoded"));
    var managed = rows.findById(credential.id()).orElseThrow();
    credential.registerFailure(
        Instant.parse("2026-10-12T12:00:00Z"), new PinPolicy(1, Duration.ofMinutes(15)));
    credentials.save(credential);
    assertThat(rows.findById(credential.id()).orElseThrow()).isSameAs(managed);
    assertThat(managed.getFailedAttempts()).isEqualTo(1);
    assertThat(managed.getLockedUntil()).isEqualTo(Instant.parse("2026-10-12T12:15:00Z"));
    credential.registerSuccess();
    credentials.save(credential);
    assertThat(managed.getFailedAttempts()).isZero();
    assertThat(managed.getLockedUntil()).isNull();
    assertThat(managed.getPinHash()).isEqualTo("encoded");
  }

  @Test
  void credentialCannotBeReassignedToAnotherAdult() {
    var credential =
        credentials.save(PinCredential.register(UUID.randomUUID().toString(), "encoded"));
    var otherOwner =
        PinCredential.rehydrate(
            credential.id(), UUID.randomUUID().toString(), "replacement", 3, null);
    assertThatThrownBy(() -> credentials.save(otherOwner))
        .isInstanceOf(InvalidDataAccessApiUsageException.class)
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    PinCredentialPersistenceEntity original = rows.findById(credential.id()).orElseThrow();
    assertThat(original.getOlderAdultId()).isEqualTo(credential.olderAdultId());
    assertThat(original.getPinHash()).isEqualTo("encoded");
    assertThat(original.getFailedAttempts()).isZero();
  }
}
