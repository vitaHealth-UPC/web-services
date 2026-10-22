package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

/** Checks technical audit timestamps without altering account verification expiry. */
@SpringBootTest
@ActiveProfiles("test")
class AccountPersistenceAuditingIntegrationTest {
  @Autowired private AccountJpaRepository accounts;
  @Autowired private AccountRepository domainAccounts;
  @Autowired private PinCredentialRepository pins;

  @Test
  void mutationReadsRequireCallerTransaction() {
    assertThrows(
        IllegalTransactionStateException.class, () -> pins.findForAuthentication("missing-adult"));
    assertThrows(
        IllegalTransactionStateException.class,
        () -> domainAccounts.findByIdForUpdate(UUID.randomUUID().toString()));
    assertThrows(
        IllegalTransactionStateException.class,
        () -> domainAccounts.findByEmailForUpdate("missing@example.com"));
  }

  @Test
  void adapterUpdatePreservesStoredAuditAndRenewedChallenge() {
    var account =
        Account.register(
            "Rosa",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "password-hash",
            "initial-hash",
            Instant.parse("2030-01-01T00:00:00Z"),
            Duration.ofMinutes(15));
    domainAccounts.save(account);
    var before = accounts.findById(account.id()).orElseThrow();
    Instant firstPersistence = before.getPersistedAt();
    account.renewVerificationCode(
        "renewed-hash", Instant.parse("2030-01-01T00:10:00Z"), Duration.ofMinutes(15));
    domainAccounts.save(account);
    var after = accounts.findById(account.id()).orElseThrow();
    assertThat(after.getPersistedAt()).isEqualTo(firstPersistence);
    assertThat(after.getModifiedAt()).isAfterOrEqualTo(firstPersistence);
    assertThat(after.getVerificationCodeHash()).isEqualTo("renewed-hash");
    assertThat(after.getVerificationExpiresAt()).isEqualTo(Instant.parse("2030-01-01T00:25:00Z"));
    accounts.deleteById(account.id());
  }

  @Test
  void recordsPersistenceTimesAndPreservesDomainExpiryOnUpdate() {
    var id = UUID.randomUUID().toString();
    var expiresAt = Instant.parse("2030-01-02T03:04:05Z");
    var saved = accounts.saveAndFlush(account(id, expiresAt, "Original"));
    // Compare stored values across the update, preserving the database timestamp precision.
    var persistedAt = accounts.findById(id).orElseThrow().getPersistedAt();

    assertThat(persistedAt).isNotNull();
    assertThat(saved.getModifiedAt()).isNotNull();
    assertThat(saved.getVerificationExpiresAt()).isEqualTo(expiresAt);

    accounts.saveAndFlush(account(id, expiresAt, "Updated"));
    var updated = accounts.findById(id).orElseThrow();
    assertThat(updated.getPersistedAt()).isEqualTo(persistedAt);
    assertThat(updated.getModifiedAt()).isAfterOrEqualTo(persistedAt);
    assertThat(updated.getName()).isEqualTo("Updated");
    assertThat(updated.getVerificationExpiresAt()).isEqualTo(expiresAt);
  }

  private AccountPersistenceEntity account(String id, Instant expiresAt, String name) {
    return new AccountPersistenceEntity(
        id,
        name,
        id + "@example.com",
        "hash",
        AccountStatus.PENDING_VERIFICATION,
        "challenge-hash",
        expiresAt,
        null,
        null,
        null);
  }
}
