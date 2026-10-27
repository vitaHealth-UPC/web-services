package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicatePushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Exercises real storage identity, uniqueness and managed rotations independently of Firebase. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushDevicePersistenceIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-17T15:00:00Z");
  @Autowired PushDeviceRegistrationRepository devices;
  @Autowired EntityManager entities;

  private PushDeviceRegistration device() {
    return PushDeviceRegistration.register(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        NOW);
  }

  @Test
  void savedRotationRetainsOwnershipAndAuditAfterReload() {
    var original = devices.save(device());
    entities.clear();
    var locked = devices.findByInstallationIdForUpdate(original.installationId()).orElseThrow();
    var rotated =
        devices.save(locked.rotate("replacement:" + UUID.randomUUID(), NOW.plusSeconds(1)));
    entities.clear();
    var stored = devices.findByInstallationId(original.installationId()).orElseThrow();
    assertThat(stored).isEqualTo(rotated);
    assertThat(stored.sessionId()).isEqualTo(original.sessionId());
    assertThat(stored.createdAt()).isEqualTo(NOW);
    assertThat(stored.updatedAt()).isEqualTo(NOW.plusSeconds(1));
  }

  @Test
  void detachedSnapshotCannotReplaceSessionOwnerOrCreationTime() {
    var original = devices.save(device());
    var forged =
        new PushDeviceRegistration(
            original.installationId(), "different-session", "replacement", NOW, NOW);
    assertThatThrownBy(() -> devices.save(forged))
        .isInstanceOf(DuplicatePushDeviceRegistration.class);
    assertThat(devices.findByInstallationId(original.installationId())).contains(original);
  }

  @Test
  void sameTokenCannotBelongToTwoInstallations() {
    var original = devices.save(device());
    var duplicate =
        PushDeviceRegistration.register(
            UUID.randomUUID().toString(), "different-session", original.token(), NOW);
    assertThatThrownBy(() -> devices.save(duplicate))
        .isInstanceOf(DuplicatePushDeviceRegistration.class);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void reservationCannotEscapeCallerTransaction() {
    assertThatThrownBy(() -> devices.findByInstallationIdForUpdate(UUID.randomUUID().toString()))
        .isInstanceOf(IllegalTransactionStateException.class);
  }
}
