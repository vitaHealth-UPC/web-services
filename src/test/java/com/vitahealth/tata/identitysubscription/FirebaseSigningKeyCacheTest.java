package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.firebase.FirebaseSigningKeyCache;
import io.jsonwebtoken.Jwts;
import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class FirebaseSigningKeyCacheTest {
  @Test void cachesUntilExpiryAndUnknownKidsCannotTriggerNetworkStorm() {
    var now = new AtomicReference<>(Instant.parse("2026-10-26T12:00:00Z"));
    var clock = new Clock() {
      public ZoneId getZone() { return ZoneOffset.UTC; }
      public Clock withZone(ZoneId zone) { return this; }
      public Instant instant() { return now.get(); }
    };
    var key = Jwts.SIG.RS256.keyPair().build().getPublic();
    var calls = new AtomicInteger();
    var cache = new FirebaseSigningKeyCache(() -> { calls.incrementAndGet(); return new FirebaseSigningKeyCache.Snapshot(Map.of("trusted", key), Duration.ofSeconds(60)); }, clock);
    assertSame(key, cache.apply("trusted"));
    for (int i = 0; i < 100; i++) assertNull(cache.apply("unknown-" + i));
    assertEquals(1, calls.get());
    now.set(now.get().plusSeconds(60));
    assertSame(key, cache.apply("trusted")); assertEquals(2, calls.get());
  }
  @Test void unavailableSourceCannotReturnExpiredTrust() {
    var cache = new FirebaseSigningKeyCache(() -> { throw new IllegalStateException("Unavailable"); }, Clock.systemUTC());
    assertThrows(IllegalStateException.class, () -> cache.apply("trusted"));
    assertNull(cache.apply(""));
  }
}
