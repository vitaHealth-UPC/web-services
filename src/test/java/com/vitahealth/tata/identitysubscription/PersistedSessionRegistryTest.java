package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence.PersistedSessionRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

/** Checks authentication rejection when the referenced care relationship no longer exists. */
class PersistedSessionRegistryTest {
  private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

  @Test
  void anotherAuthorizedRelationshipCannotValidateARevokedCredentialScope() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var accounts = mock(AccountRepository.class);
    var links = mock(CareLinkContextFacade.class);
    when(sessions.findByTokenHash(anyString()))
        .thenReturn(
            Optional.of(
                new AccountSessionPersistenceEntity(
                    "adult", "digest", NOW.plusSeconds(60), "OLDER_ADULT", "revoked")));
    when(accounts.findById("caregiver")).thenReturn(Optional.of(activeCaregiver()));
    when(links.findById("revoked"))
        .thenReturn(
            Optional.of(
                new CareLinkResult(
                    "revoked",
                    "caregiver",
                    "adult",
                    CareLinkStatus.REVOKED,
                    "TATA-4821",
                    NOW.plusSeconds(60),
                    NOW.minusSeconds(30),
                    false,
                    NOW,
                    NOW.minusSeconds(20))));
    when(links.isAuthorized("caregiver", "adult")).thenReturn(true);
    var registry =
        new PersistedSessionRegistry(sessions, accounts, links, Clock.fixed(NOW, ZoneOffset.UTC));
    assertThat(registry.authenticate("credential")).isEmpty();
  }

  @Test
  void onboardingCredentialsExpireWhenTheirRelationshipLeavesAwaitingConsent() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var accounts = mock(AccountRepository.class);
    var links = mock(CareLinkContextFacade.class);
    when(sessions.findByTokenHash(anyString()))
        .thenReturn(
            Optional.of(
                new AccountSessionPersistenceEntity(
                    "adult", "digest", NOW.plusSeconds(60), "LINK_SETUP", "link")));
    when(accounts.findById("caregiver")).thenReturn(Optional.of(activeCaregiver()));
    var registry =
        new PersistedSessionRegistry(sessions, accounts, links, Clock.fixed(NOW, ZoneOffset.UTC));
    for (var status : new CareLinkStatus[] {CareLinkStatus.CONFIRMED, CareLinkStatus.REVOKED}) {
      when(links.findById("link"))
          .thenReturn(
              Optional.of(
                  new CareLinkResult(
                      "link",
                      "caregiver",
                      "adult",
                      status,
                      "TATA-4821",
                      NOW.plusSeconds(60),
                      NOW.minusSeconds(30),
                      status == CareLinkStatus.CONFIRMED,
                      NOW.minusSeconds(20),
                      status == CareLinkStatus.CONFIRMED ? NOW.minusSeconds(20) : null)));
      assertThat(registry.authenticate("credential")).isEmpty();
    }
  }

  private Account activeCaregiver() {
    var account =
        Account.register(
            "Caregiver",
            new EmailAddress("caregiver@example.com"),
            "hash",
            "challenge",
            NOW,
            Duration.ofMinutes(15));
    account.completeVerification();
    return account;
  }

  @Test
  void registrationRejectsCredentialsThatLookupCannotAcceptBeforeWriting() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var registry = registry(sessions);
    var identity = new AuthenticatedSession("caregiver", Role.CAREGIVER, NOW.plusSeconds(60), null);

    for (String token : new String[] {null, "", "   ", "x".repeat(4097)}) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> registry.register("session", token, identity));
      assertThat(registry.authenticate(token)).isEmpty();
      registry.revoke(token);
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> registry.register(" ", "credential", identity));
    assertThat(registry.authenticate("credential", " ")).isEmpty();
    verifyNoInteractions(sessions);
  }

  @Test
  void registrationRejectsExpiredSessionsIncludingTheExactExpirationBoundary() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var registry = registry(sessions);
    for (Instant expiry : new Instant[] {NOW.minusSeconds(1), NOW}) {
      var identity = new AuthenticatedSession("caregiver", Role.CAREGIVER, expiry, null);
      assertThatIllegalArgumentException()
          .isThrownBy(() -> registry.register("session", "credential", identity));
    }
    verifyNoInteractions(sessions);
  }

  @Test
  void registrationPreservesSignedIdentityAndStoresOnlyTheCredentialDigest() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var identity = new AuthenticatedSession("adult", Role.OLDER_ADULT, NOW.plusSeconds(60), "link");
    registry(sessions).register("signed-reference", "credential", identity);

    var saved = ArgumentCaptor.forClass(AccountSessionPersistenceEntity.class);
    verify(sessions).save(saved.capture());
    assertThat(saved.getValue().getId()).isEqualTo("signed-reference");
    assertThat(saved.getValue().getAccountId()).isEqualTo(identity.subjectId());
    assertThat(saved.getValue().getRole()).isEqualTo(identity.role().name());
    assertThat(saved.getValue().getCareLinkId()).isEqualTo(identity.careLinkId());
    assertThat(saved.getValue().getExpiresAt()).isEqualTo(identity.expiresAt());
    assertThat(saved.getValue().getTokenHash())
        .isEqualTo("e265b6f564601a1fe8dc42785cd18a868bd8013eb5899560e79248767a683e6b");
  }

  @Test
  void expiredSessionDoesNotQueryLiveAccountOrConsent() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var accounts = mock(AccountRepository.class);
    var links = mock(CareLinkContextFacade.class);
    var row =
        new AccountSessionPersistenceEntity(
            "session", "adult", "digest", NOW, "OLDER_ADULT", "link");
    when(sessions.findByTokenHash(anyString())).thenReturn(Optional.of(row));
    var registry =
        new PersistedSessionRegistry(sessions, accounts, links, Clock.fixed(NOW, ZoneOffset.UTC));
    assertThat(registry.authenticate("credential")).isEmpty();
    assertThat(registry.authenticate("credential", "session")).isEmpty();
    verifyNoInteractions(accounts, links);
  }

  private PersistedSessionRegistry registry(AccountSessionJpaRepository sessions) {
    return new PersistedSessionRegistry(
        sessions,
        mock(AccountRepository.class),
        mock(CareLinkContextFacade.class),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void relationshipLookupFailureRemainsVisibleInsteadOfBecomingInvalidCredentials() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var links = mock(CareLinkContextFacade.class);
    var row =
        new AccountSessionPersistenceEntity(
            "adult", "digest", NOW.plusSeconds(60), "OLDER_ADULT", "link");
    when(sessions.findByTokenHash(anyString())).thenReturn(Optional.of(row));
    var failure = new DataAccessResourceFailureException("database unavailable");
    when(links.findById("link")).thenThrow(failure);
    var registry =
        new PersistedSessionRegistry(
            sessions, mock(AccountRepository.class), links, Clock.fixed(NOW, ZoneOffset.UTC));
    assertThatThrownBy(() -> registry.authenticate("credential")).isSameAs(failure);
  }

  @Test
  void missingCareLinkInvalidatesRegisteredSession() {
    var now = Instant.parse("2026-10-12T12:00:00Z");
    var sessions = mock(AccountSessionJpaRepository.class);
    var links = mock(CareLinkContextFacade.class);
    var row =
        new AccountSessionPersistenceEntity(
            "adult", "digest", now.plusSeconds(60), "OLDER_ADULT", "missing-link");
    when(sessions.findByTokenHash(anyString())).thenReturn(Optional.of(row));
    when(links.findById("missing-link")).thenReturn(Optional.empty());
    var registry =
        new PersistedSessionRegistry(
            sessions, mock(AccountRepository.class), links, Clock.fixed(now, ZoneOffset.UTC));
    assertThat(registry.authenticate("credential")).isEmpty();
  }

  @Test
  void registrationReferenceRechecksRevocationExpiryAndCurrentAccountState() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var accounts = mock(AccountRepository.class);
    var links = mock(CareLinkContextFacade.class);
    var registry =
        new PersistedSessionRegistry(sessions, accounts, links, Clock.fixed(NOW, ZoneOffset.UTC));
    var row =
        new AccountSessionPersistenceEntity(
            "session-1", "caregiver", "digest", NOW.plusSeconds(60), "CAREGIVER", null);
    when(sessions.findById("session-1")).thenReturn(Optional.of(row));
    when(accounts.findById("caregiver")).thenReturn(Optional.of(activeCaregiver()));
    assertThat(registry.findActiveRegistration("session-1"))
        .hasValueSatisfying(
            result -> {
              assertThat(result.sessionId()).isEqualTo("session-1");
              assertThat(result.identity().subjectId()).isEqualTo("caregiver");
            });
    when(accounts.findById("caregiver")).thenReturn(Optional.empty());
    assertThat(registry.findActiveRegistration("session-1")).isEmpty();
    when(sessions.findById("session-1")).thenReturn(Optional.empty());
    assertThat(registry.findActiveRegistration("session-1")).isEmpty();
    when(sessions.findById("session-1"))
        .thenReturn(
            Optional.of(
                new AccountSessionPersistenceEntity(
                    "session-1", "caregiver", "digest", NOW, "CAREGIVER", null)));
    assertThat(registry.findActiveRegistration("session-1")).isEmpty();
  }

  @Test
  void invalidRegistrationLookupsNeverReadStorage() {
    var sessions = mock(AccountSessionJpaRepository.class);
    var registry = registry(sessions);
    assertThat(registry.findRegistration(null)).isEmpty();
    assertThat(registry.findRegistration("x".repeat(4097))).isEmpty();
    assertThat(registry.findActiveRegistration(null)).isEmpty();
    assertThat(registry.findActiveRegistration(" ")).isEmpty();
    verifyNoInteractions(sessions);
  }
}
