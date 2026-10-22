package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.shared.infrastructure.time.ClockConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Reproduces rounding at a second boundary and verifies aligned application occurrence times. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CareLinkTimestampPrecisionIntegrationTest {
  @Autowired private CareLinkRepository links;
  @Autowired private EntityManager entityManager;
  private final Instant raw = Instant.parse("2026-10-13T12:00:00.999999999Z");

  private CareLink persistedAccepted(Instant now, String code) {
    var link =
        CareLink.createPending(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            LinkingCode.issue(code, now, Duration.ofMinutes(15)),
            now);
    link.accept(code, now);
    links.save(link);
    entityManager.flush();
    entityManager.clear();
    return links.findById(link.id()).orElseThrow();
  }

  @Test
  void unalignedSourceCanRoundStoredStateAheadOfItsOwnInstant() {
    var restored = persistedAccepted(raw, "TATA-9011");
    assertTrue(restored.updatedAt().isAfter(raw));
    assertThrows(IllegalArgumentException.class, () -> restored.registerConsent(true, raw));
  }

  @Test
  void microsecondAlignedSourcePreservesSameInstantTransitionsAfterReload() {
    var clock = ClockConfiguration.withDatabasePrecision(Clock.fixed(raw, ZoneOffset.UTC));
    var now = clock.instant();
    assertEquals(0, now.getNano() % 1000);
    var restored = persistedAccepted(now, "TATA-9012");
    assertEquals(now, restored.updatedAt());
    restored.registerConsent(true, clock.instant());
    restored.confirm(clock.instant());
    assertTrue(links.save(restored).isActive());
  }
}
