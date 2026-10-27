package com.vitahealth.tata.identitysubscription.infrastructure.tokens.firebase;

import java.security.PublicKey;
import java.time.*;
import java.util.*;
import java.util.function.*;

/** Trusts only a bounded-lifetime snapshot from the configured Google certificate source. */
public final class FirebaseSigningKeyCache implements Function<String, PublicKey> {
  public record Snapshot(Map<String, PublicKey> keys, Duration lifetime) {
    public Snapshot {
      keys = Map.copyOf(keys);
      if (keys.isEmpty() || lifetime == null || lifetime.isNegative() || lifetime.isZero())
        throw new IllegalArgumentException("Invalid Firebase certificate snapshot");
    }
  }
  private final Supplier<Snapshot> source;
  private final Clock clock;
  private Map<String, PublicKey> keys = Map.of();
  private Instant expiresAt = Instant.MIN;
  public FirebaseSigningKeyCache(Supplier<Snapshot> source, Clock clock) {
    this.source = Objects.requireNonNull(source); this.clock = Objects.requireNonNull(clock);
  }
  @Override public synchronized PublicKey apply(String kid) {
    if (kid == null || kid.isBlank() || kid.length() > 256) return null;
    if (!clock.instant().isBefore(expiresAt)) {
      var snapshot = source.get();
      keys = snapshot.keys();
      expiresAt = clock.instant().plus(snapshot.lifetime().compareTo(Duration.ofHours(24)) > 0 ? Duration.ofHours(24) : snapshot.lifetime());
    }
    // Unknown kids cannot force network refreshes while a valid snapshot is cached.
    return keys.get(kid);
  }
}
