package com.tata.identitysubscription.infrastructure.security;

import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import com.tata.identitysubscription.application.models.AuthenticatedSubject;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class OpaqueSessionTokenService implements SessionTokenService {
    private final AccountSessionJpaRepository sessions;
    private final Clock clock;

    public OpaqueSessionTokenService(AccountSessionJpaRepository sessions) {
        this(sessions, Clock.systemUTC());
    }

    OpaqueSessionTokenService(AccountSessionJpaRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public SessionResult issue(String accountId) {
        var rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        var expiresAt = clock.instant().plus(12, ChronoUnit.HOURS);
        sessions.save(new AccountSessionPersistenceEntity(accountId, sha256(rawToken), expiresAt));
        return new SessionResult(accountId, rawToken, expiresAt);
    }

    @Override
    public Optional<AuthenticatedSubject> authenticate(String rawAccessToken) {
        if (rawAccessToken == null || rawAccessToken.isBlank()) {
            return Optional.empty();
        }
        return sessions.findByTokenHashAndExpiresAtAfter(sha256(rawAccessToken.trim()), clock.instant())
                .map(session -> new AuthenticatedSubject(session.getAccountId()));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
