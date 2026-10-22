package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt.JwtTokenCodec;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.opaque.OpaqueSessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence.PersistedSessionRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifies JWT issuance, persisted revocation and strict compatibility with legacy credentials. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JwtSessionIntegrationTest {
  @Autowired private SessionTokenService tokens;
  @Autowired private OpaqueSessionTokenService legacy;
  @Autowired private JwtTokenCodec codec;
  @Autowired private PersistedSessionRegistry registry;
  @Autowired private AccountRepository accounts;
  @Autowired private AccountSessionJpaRepository sessions;

  @Test
  void issuesSignedCredentialAndRevocationImmediatelyRejectsIt() {
    var owner = account();
    var result = tokens.issue(owner.id());
    assertThat(codec.decode(result.accessToken())).isPresent();
    assertThat(tokens.authenticate(result.accessToken())).isPresent();
    tokens.revoke(result.accessToken());
    assertThat(tokens.authenticate(result.accessToken())).isEmpty();
  }

  @Test
  void recognizesExistingOpaqueCredentialAndKeepsItsRevocation() {
    var result = legacy.issue(account().id());
    assertThat(tokens.authenticate(result.accessToken())).isPresent();
    tokens.revoke(result.accessToken());
    assertThat(tokens.authenticate(result.accessToken())).isEmpty();
  }

  @Test
  void signedClaimsCannotOverrideDifferentPersistedIdentity() {
    var signedOwner = account();
    var storedOwner = account();
    var sessionId = UUID.randomUUID().toString();
    var expiry = Instant.now().plusSeconds(600).truncatedTo(ChronoUnit.SECONDS);
    var token =
        codec.encode(
            sessionId, new AuthenticatedSession(signedOwner.id(), Role.CAREGIVER, expiry, null));
    registry.register(
        sessionId, token, new AuthenticatedSession(storedOwner.id(), Role.CAREGIVER, expiry, null));

    assertThat(codec.decode(token)).isPresent();
    assertThat(registry.authenticate(token, sessionId)).isPresent();
    assertThat(tokens.authenticate(token)).isEmpty();
  }

  @Test
  void malformedJwtCannotAuthenticateEvenWhenItsHashIsInDatabase() throws Exception {
    var owner = account();
    var malformed = "bad.jwt.token";
    var hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(malformed.getBytes(StandardCharsets.UTF_8)));
    sessions.saveAndFlush(
        new AccountSessionPersistenceEntity(owner.id(), hash, Instant.now().plusSeconds(600)));
    assertThat(registry.authenticate(malformed)).isPresent();
    assertThat(tokens.authenticate(malformed)).isEmpty();
  }

  private Account account() {
    var account =
        Account.register(
            "Owner",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "password-hash",
            "challenge-hash",
            Instant.now(),
            Duration.ofMinutes(15));
    account.completeVerification();
    return accounts.save(account);
  }
}
