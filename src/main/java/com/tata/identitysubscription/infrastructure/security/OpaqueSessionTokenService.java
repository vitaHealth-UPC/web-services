package com.tata.identitysubscription.infrastructure.security;

import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OpaqueSessionTokenService implements SessionTokenService {
    private final AccountSessionJpaRepository sessions;

    public OpaqueSessionTokenService(AccountSessionJpaRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public SessionResult issue(String accountId) {
        var rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        var expiresAt = Instant.now().plus(12, ChronoUnit.HOURS);
        sessions.save(new AccountSessionPersistenceEntity(accountId, sha256(rawToken), expiresAt));
        return new SessionResult(accountId, rawToken, expiresAt);
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
