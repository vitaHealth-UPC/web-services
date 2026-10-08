package com.tata.identitysubscription.application.internal.commandservices;

import com.tata.identitysubscription.application.commandservices.PinCommandService;
import com.tata.identitysubscription.application.IdentityApplicationException;
import com.tata.identitysubscription.application.internal.outboundservices.PasswordHasher;
import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.tata.identitysubscription.domain.model.valueobjects.PinPolicy;
import com.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class PinCommandServiceImpl implements PinCommandService {
    private final PinCredentialRepository repository;
    private final PasswordHasher passwordHasher;
    private final SessionTokenService sessionTokenService;
    private final Clock clock;
    private final PinPolicy policy;

    @Autowired
    public PinCommandServiceImpl(
            PinCredentialRepository repository,
            PasswordHasher passwordHasher,
            SessionTokenService sessionTokenService
    ) {
        this(repository, passwordHasher, sessionTokenService, Clock.systemUTC(), PinPolicy.defaultPolicy());
    }

    PinCommandServiceImpl(
            PinCredentialRepository repository,
            PasswordHasher passwordHasher,
            SessionTokenService sessionTokenService,
            Clock clock,
            PinPolicy policy
    ) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.sessionTokenService = sessionTokenService;
        this.clock = clock;
        this.policy = policy;
    }

    @Override
    public void register(RegisterPinCommand command) {
        validatePin(command.pin());
        if (repository.findByOlderAdultId(command.olderAdultId()).isPresent()) {
            throw error(IdentityApplicationException.Code.PIN_ALREADY_REGISTERED, "PIN already registered");
        }
        repository.save(PinCredential.register(command.olderAdultId(), passwordHasher.hash(command.pin())));
    }

    @Override
    @Transactional(noRollbackFor = IdentityApplicationException.class)
    public SessionResult authenticate(AuthenticateWithPinCommand command) {
        validatePin(command.pin());
        var credential = repository.findForAuthentication(command.olderAdultId())
                .orElseThrow(() -> error(IdentityApplicationException.Code.PIN_NOT_FOUND, "PIN credential not found"));

        var now = clock.instant();
        if (credential.isLockedAt(now)) {
            throw error(IdentityApplicationException.Code.PIN_LOCKED, "PIN access is temporarily locked");
        }

        if (!passwordHasher.matches(command.pin(), credential.pinHash())) {
            credential.registerFailure(now, policy);
            repository.save(credential);
            if (credential.isLockedAt(now.plusMillis(1))) {
                throw error(IdentityApplicationException.Code.PIN_LOCKED, "PIN access is temporarily locked");
            }
            throw error(IdentityApplicationException.Code.INVALID_PIN, "PIN is incorrect");
        }

        credential.registerSuccess();
        repository.save(credential);
        return sessionTokenService.issueOlderAdult(command.olderAdultId());
    }

    private static void validatePin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must contain exactly 4 digits");
        }
    }

    private static IdentityApplicationException error(IdentityApplicationException.Code code, String message) {
        return new IdentityApplicationException(code, message);
    }
}
