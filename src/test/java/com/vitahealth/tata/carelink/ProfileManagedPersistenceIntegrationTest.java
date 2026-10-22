package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.OlderAdultProfileJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies managed updates preserve adult profile identity and serialize command reservations. */
@SpringBootTest
@ActiveProfiles("test")
class ProfileManagedPersistenceIntegrationTest {
  @Autowired private OlderAdultProfileRepository profiles;
  @Autowired private OlderAdultProfileJpaRepository rows;
  @Autowired private PlatformTransactionManager transactions;

  @Test
  void profileUpdateKeepsManagedIdentityAndCreationWhilePersistingBasicDataAndContact() {
    var created = Instant.parse("2026-10-13T12:00:00Z");
    var profile =
        profiles.save(
            OlderAdultProfile.register(
                "caregiver",
                new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)),
                null,
                created));
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var loaded = profiles.findByIdForUpdate(profile.id()).orElseThrow();
              var managed = rows.findById(profile.id()).orElseThrow();
              loaded.updateBasicData(
                  new OlderAdultBasicData("Rosa Vargas", LocalDate.of(1958, 1, 1)),
                  created.plusSeconds(1));
              loaded.associateEmergencyContact(
                  new EmergencyContact("Diego", "Hijo", "999111222"), created.plusSeconds(2));
              profiles.save(loaded);
              assertThat(rows.findById(profile.id()).orElseThrow()).isSameAs(managed);
              assertThat(managed.getCreatedAt()).isEqualTo(created);
            });
    var restored = profiles.findById(profile.id()).orElseThrow();
    assertThat(restored.basicData().fullName()).isEqualTo("Rosa Vargas");
    assertThat(restored.emergencyContact().name()).isEqualTo("Diego");
    assertThat(restored.registeredByCaregiverId()).isEqualTo("caregiver");
    assertThat(restored.updatedAt()).isEqualTo(created.plusSeconds(2));
  }

  @Test
  void persistenceRejectsReplacementCreationEvidenceWithoutChangingProfileData() {
    var now = Instant.parse("2026-10-13T12:00:00Z");
    var original =
        profiles.save(
            OlderAdultProfile.register(
                "owner", new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)), null, now));
    var altered =
        OlderAdultProfile.rehydrate(
            original.id(),
            "owner",
            new OlderAdultBasicData("Changed", LocalDate.of(1958, 1, 1)),
            null,
            now.plusSeconds(1),
            now.plusSeconds(2));
    assertThatThrownBy(() -> profiles.save(altered))
        .isInstanceOf(InvalidDataAccessApiUsageException.class);
    var restored = profiles.findById(original.id()).orElseThrow();
    assertThat(restored.createdAt()).isEqualTo(now);
    assertThat(restored.basicData().fullName()).isEqualTo("Rosa");
    assertThat(restored.updatedAt()).isEqualTo(now);
  }

  @Test
  void persistenceRejectsProfileReassignmentWithoutChangingStoredOwnership() {
    var now = Instant.parse("2026-10-13T12:00:00Z");
    var original =
        profiles.save(
            OlderAdultProfile.register(
                "owner", new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)), null, now));
    var reassigned =
        OlderAdultProfile.rehydrate(
            original.id(), "foreign", original.basicData(), null, now, now.plusSeconds(1));
    assertThatThrownBy(() -> profiles.save(reassigned))
        .isInstanceOf(InvalidDataAccessApiUsageException.class);
    assertThat(profiles.findById(original.id()).orElseThrow().registeredByCaregiverId())
        .isEqualTo("owner");
    assertThatThrownBy(() -> profiles.findByIdForUpdate(original.id()))
        .isInstanceOf(IllegalTransactionStateException.class);
  }
}
