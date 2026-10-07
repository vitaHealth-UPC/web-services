package com.tata.identitysubscription.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OpaqueSessionTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");

    @Autowired AccountSessionJpaRepository sessions;

    @Test
    void authenticatesIssuedTokenAndRejectsUnknownToken() {
        var service = new OpaqueSessionTokenService(sessions, Clock.fixed(NOW, ZoneOffset.UTC));
        var issued = service.issue("account-1");

        assertEquals("account-1", service.authenticate(issued.accessToken()).orElseThrow().subjectId());
        assertTrue(service.authenticate("missing.token").isEmpty());
        assertTrue(service.authenticate(" ").isEmpty());
    }

    @Test
    void rejectsExpiredSession() {
        sessions.save(new AccountSessionPersistenceEntity(
                "account-1",
                "deadbeef",
                NOW.minusSeconds(1)
        ));
        var service = new OpaqueSessionTokenService(sessions, Clock.fixed(NOW, ZoneOffset.UTC));
        assertEquals(Optional.empty(), sessions.findByTokenHashAndExpiresAtAfter("deadbeef", NOW));
        assertTrue(service.authenticate("anything").isEmpty());
    }
}
