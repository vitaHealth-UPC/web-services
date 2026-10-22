package com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.opaque.OpaqueSessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence.PersistedSessionRegistry;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;

/** Signed sessions backed by persisted revocation and current account/consent authorization. */
@Transactional
public class JwtSessionTokenService implements SessionTokenService {
  private static final int STANDARD_SESSION_MINUTES = 720;
  private static final int ONBOARDING_SESSION_MINUTES = 15;
  private static final Pattern LEGACY =
      Pattern.compile(
          "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\."
              + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  private final JwtTokenCodec codec;
  private final OpaqueSessionTokenService legacy;
  private final PersistedSessionRegistry registry;
  private final CareLinkContextFacade links;
  private final Clock clock;

  /** Injects signing, persistence and existing live-authorization checks. */
  public JwtSessionTokenService(
      JwtTokenCodec codec,
      OpaqueSessionTokenService legacy,
      CareLinkContextFacade links,
      Clock clock,
      PersistedSessionRegistry registry) {
    this.codec = codec;
    this.legacy = legacy;
    this.links = links;
    this.clock = clock;
    this.registry = registry;
  }

  /** Signs a caregiver session using the standard twelve-hour lifetime. */
  @Override
  public SessionResult issue(String accountId) {
    return issue(accountId, Role.CAREGIVER, null, STANDARD_SESSION_MINUTES);
  }

  /** Signs a fifteen-minute session scoped to one consent onboarding relationship. */
  @Override
  public SessionResult issueLinkSetup(String olderAdultId, String careLinkId) {
    return issue(olderAdultId, Role.LINK_SETUP, careLinkId, ONBOARDING_SESSION_MINUTES);
  }

  /** Resolves a confirmed care relationship before signing adult access to its scope. */
  @Override
  public SessionResult issueOlderAdult(String olderAdultId) {
    var profile =
        links
            .findOlderAdult(olderAdultId)
            .orElseThrow(() -> new IllegalArgumentException("older adult profile not found"));
    var link =
        links.getConfirmedByCaregiver(profile.registeredByCaregiverId()).stream()
            .filter(item -> olderAdultId.equals(item.olderAdultId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("a confirmed care link is required"));
    return issue(olderAdultId, Role.OLDER_ADULT, link.id(), STANDARD_SESSION_MINUTES);
  }

  /** Checks current consent and subject ownership before issuing for this exact relationship. */
  @Override
  public SessionResult issueOlderAdult(String olderAdultId, String careLinkId) {
    var link = links.getById(careLinkId);
    if (!olderAdultId.equals(link.olderAdultId())
        || link.status() != CareLinkStatus.CONFIRMED
        || !link.consentGranted()
        || link.confirmedAt() == null) {
      throw new IllegalArgumentException("a confirmed care link for this adult is required");
    }
    return issue(olderAdultId, Role.OLDER_ADULT, link.id(), 720);
  }

  private SessionResult issue(String subject, Role role, String careLinkId, int minutes) {
    var expiry = clock.instant().plus(minutes, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
    var sessionId = UUID.randomUUID().toString();
    var identity = new AuthenticatedSession(subject, role, expiry, careLinkId);
    var raw = codec.encode(sessionId, identity);
    registry.register(sessionId, raw, identity);
    return new SessionResult(subject, raw, expiry);
  }

  /** Requires signed claims and stored authorization to describe the same session identity. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AuthenticatedSession> authenticate(String token) {
    if (token == null || token.length() > 4096) return Optional.empty();
    if (LEGACY.matcher(token).matches()) return legacy.authenticate(token);
    return codec
        .decode(token)
        .flatMap(
            claims ->
                registry
                    .authenticate(token, claims.sessionId())
                    .filter(identity -> identity.equals(claims.identity())));
  }

  /** Revokes both signed and supported legacy credentials through their persisted digest. */
  @Override
  public void revoke(String token) {
    registry.revoke(token);
  }
}
