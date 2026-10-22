package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.IdentityApplicationException;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinCode;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinPolicy;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles PIN authenticate with explicit transactional ownership. */
@Service
@Transactional(noRollbackFor = IdentityApplicationException.class)
public class AuthenticatePinCommandHandler {
  private final PinCredentialRepository repository;
  private final PasswordHasher passwordHasher;
  private final SessionTokenService sessionTokenService;
  private final Clock clock;
  private final PinPolicy policy = PinPolicy.defaultPolicy();

  /** Injects dependencies for this PIN operation. */
  public AuthenticatePinCommandHandler(
      PinCredentialRepository repository,
      PasswordHasher passwordHasher,
      SessionTokenService sessionTokenService,
      Clock clock) {
    this.repository = repository;
    this.passwordHasher = passwordHasher;
    this.sessionTokenService = sessionTokenService;
    this.clock = clock;
  }

  /** Executes the PIN command; authentication failures retain the persisted attempt count. */
  public SessionResult handle(AuthenticateWithPinCommand command) {
    var pin = new PinCode(command.pin());
    var credential =
        repository
            .findForAuthentication(command.olderAdultId())
            .orElseThrow(
                () ->
                    error(
                        IdentityApplicationException.Code.PIN_NOT_FOUND,
                        "PIN credential not found"));

    var now = clock.instant();
    if (credential.isLockedAt(now)) {
      throw error(IdentityApplicationException.Code.PIN_LOCKED, "PIN access is temporarily locked");
    }

    if (!passwordHasher.matches(pin.value(), credential.pinHash())) {
      credential.registerFailure(now, policy);
      repository.save(credential);
      if (credential.isLockedAt(now)) {
        throw error(
            IdentityApplicationException.Code.PIN_LOCKED, "PIN access is temporarily locked");
      }
      throw error(IdentityApplicationException.Code.INVALID_PIN, "PIN is incorrect");
    }

    credential.registerSuccess();
    repository.save(credential);
    return sessionTokenService.issueOlderAdult(command.olderAdultId());
  }

  private static IdentityApplicationException error(
      IdentityApplicationException.Code code, String message) {
    return new IdentityApplicationException(code, message);
  }
}
