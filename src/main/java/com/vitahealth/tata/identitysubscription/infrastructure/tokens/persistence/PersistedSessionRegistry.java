package com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionRegistrationLookup;
import com.vitahealth.tata.identitysubscription.application.result.RegisteredSessionResult;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores credential hashes and verifies current authorization independently of token encoding. */
@Service
@Transactional
public class PersistedSessionRegistry implements SessionRegistrationLookup {
  private static final int MAX_CREDENTIAL_LENGTH = 4096;
  private final AccountSessionJpaRepository sessions;
  private final AccountRepository accounts;
  private final CareLinkContextFacade links;
  private final Clock clock;

  /** Injects persistence, current account/consent access and time. */
  public PersistedSessionRegistry(
      AccountSessionJpaRepository sessions,
      AccountRepository accounts,
      CareLinkContextFacade links,
      Clock clock) {
    this.sessions = sessions;
    this.accounts = accounts;
    this.links = links;
    this.clock = clock;
  }

  /**
   * Persists a credential digest with its stable session identity and role.
   *
   * @param sessionId stable reference signed into the token or assigned to an opaque session
   * @param raw credential whose digest is stored; never included in validation messages
   * @param identity verified issuance scope and expiration
   * @throws IllegalArgumentException when the reference or credential cannot be authenticated, or
   *     when the session has already expired
   */
  public void register(String sessionId, String raw, AuthenticatedSession identity) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId is required");
    }
    if (!isUsableCredential(raw)) {
      throw new IllegalArgumentException("credential is blank or exceeds the supported length");
    }
    Objects.requireNonNull(identity, "identity");
    if (!identity.expiresAt().isAfter(clock.instant())) {
      throw new IllegalArgumentException("session must expire in the future");
    }
    sessions.save(
        new AccountSessionPersistenceEntity(
            sessionId,
            identity.subjectId(),
            sha256(raw),
            identity.expiresAt(),
            identity.role().name(),
            identity.careLinkId()));
  }

  /** Matches the signed session reference before applying current database authorization. */
  @Transactional(readOnly = true)
  public Optional<AuthenticatedSession> authenticate(String raw, String sessionId) {
    if (!isUsableCredential(raw) || sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    return sessions
        .findByTokenHash(sha256(raw))
        .filter(row -> row.getId().equals(sessionId))
        .filter(row -> row.getExpiresAt().isAfter(clock.instant()))
        .flatMap(this::authorize);
  }

  /** Removes a registered credential, including legacy credentials. */
  public void revoke(String raw) {
    if (isUsableCredential(raw)) sessions.deleteByTokenHash(sha256(raw));
  }

  /**
   * Rechecks persisted revocation and live authorization after the caller verifies token format.
   */
  @Transactional(readOnly = true)
  public Optional<AuthenticatedSession> authenticate(String token) {
    if (!isUsableCredential(token)) return Optional.empty();
    return sessions
        .findByTokenHash(sha256(token))
        .filter(s -> s.getExpiresAt().isAfter(clock.instant()))
        .flatMap(this::authorize);
  }

  /** Resolves a previously authenticated credential without retaining its raw value. */
  @Override
  @Transactional(readOnly = true)
  public Optional<RegisteredSessionResult> findRegistration(String credential) {
    if (!isUsableCredential(credential)) return Optional.empty();
    return sessions.findByTokenHash(sha256(credential)).flatMap(this::activeRegistration);
  }

  /** Rejects removed, expired or currently unauthorized registration references. */
  @Override
  @Transactional(readOnly = true)
  public Optional<RegisteredSessionResult> findActiveRegistration(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) return Optional.empty();
    return sessions.findById(sessionId).flatMap(this::activeRegistration);
  }

  private Optional<RegisteredSessionResult> activeRegistration(
      AccountSessionPersistenceEntity row) {
    if (!row.getExpiresAt().isAfter(clock.instant())) return Optional.empty();
    return authorize(row).map(identity -> new RegisteredSessionResult(row.getId(), identity));
  }

  /** Applies live ownership and consent checks to one persisted session snapshot. */
  private Optional<AuthenticatedSession> authorize(AccountSessionPersistenceEntity s) {
    if (s.getRole() == null) return Optional.empty();
    final Role role;
    try {
      role = Role.valueOf(s.getRole());
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
    if (role == Role.CAREGIVER) {
      if (accounts.findById(s.getAccountId()).filter(a -> a.canAuthenticate()).isEmpty())
        return Optional.empty();
    } else {
      if (s.getCareLinkId() == null) return Optional.empty();
      var relationship = links.findById(s.getCareLinkId());
      if (relationship.isEmpty()) return Optional.empty();
      var link = relationship.orElseThrow();
      if (!s.getAccountId().equals(link.olderAdultId())
          || accounts.findById(link.caregiverId()).filter(a -> a.canAuthenticate()).isEmpty())
        return Optional.empty();
      if (role == Role.OLDER_ADULT
          && (link.status() != CareLinkStatus.CONFIRMED
              || !link.consentGranted()
              || link.confirmedAt() == null)) return Optional.empty();
      if (role == Role.LINK_SETUP
          && (link.codeUsedAt() == null || link.status() != CareLinkStatus.AWAITING_CONSENT))
        return Optional.empty();
    }
    return Optional.of(
        new AuthenticatedSession(s.getAccountId(), role, s.getExpiresAt(), s.getCareLinkId()));
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  /** Keeps issuance, lookup and revocation on the same credential boundary. */
  private static boolean isUsableCredential(String credential) {
    return credential != null
        && !credential.isBlank()
        && credential.length() <= MAX_CREDENTIAL_LENGTH;
  }
}
