package com.vitahealth.tata.identitysubscription.infrastructure.tokens.opaque;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence.PersistedSessionRegistry;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues revocable opaque tokens and rechecks account and care-link access on every lookup. */
@Service
@Transactional
public class OpaqueSessionTokenService implements SessionTokenService {
  private final CareLinkContextFacade links;
  private final Clock clock;
  private final PersistedSessionRegistry registry;

  /** Injects current care relationships, issuance time and shared credential persistence. */
  public OpaqueSessionTokenService(
      CareLinkContextFacade links, Clock clock, PersistedSessionRegistry registry) {
    this.links = links;
    this.clock = clock;
    this.registry = registry;
  }

  /** Issues a revocable caregiver credential with the standard session lifetime. */
  @Override
  public SessionResult issue(String accountId) {
    return issue(accountId, Role.CAREGIVER, null, 720);
  }

  /** Issues a short-lived credential restricted to consent onboarding. */
  @Override
  public SessionResult issueLinkSetup(String owner, String linkId) {
    return issue(owner, Role.LINK_SETUP, linkId, 15);
  }

  /** Issues adult access scoped to an existing confirmed care relationship. */
  @Override
  public SessionResult issueOlderAdult(String owner) {
    var profile =
        links
            .findOlderAdult(owner)
            .orElseThrow(() -> new IllegalArgumentException("older adult profile not found"));
    var link =
        links.getConfirmedByCaregiver(profile.registeredByCaregiverId()).stream()
            .filter(l -> owner.equals(l.olderAdultId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("a confirmed care link is required"));
    return issue(owner, Role.OLDER_ADULT, link.id(), 720);
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

  private SessionResult issue(String owner, Role role, String linkId, int minutes) {
    var raw = UUID.randomUUID() + "." + UUID.randomUUID();
    var expiry = clock.instant().plus(minutes, ChronoUnit.MINUTES);
    registry.register(
        UUID.randomUUID().toString(), raw, new AuthenticatedSession(owner, role, expiry, linkId));
    return new SessionResult(owner, raw, expiry);
  }

  /** Resolves an opaque credential through persisted expiration and current authorization. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AuthenticatedSession> authenticate(String token) {
    if (token == null || token.isBlank() || token.length() > 256) return Optional.empty();
    return registry.authenticate(token);
  }

  /** Removes the credential digest so subsequent authentication cannot resolve it. */
  @Override
  public void revoke(String token) {
    registry.revoke(token);
  }
}
