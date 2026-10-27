package com.vitahealth.tata.identitysubscription.infrastructure.tokens.firebase;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.SocialIdentityVerifier;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerifiedSocialIdentity;
import io.jsonwebtoken.*;
import java.security.PublicKey;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;

/**
 * Validates Firebase signed evidence; certificate retrieval is an explicit infrastructure
 * dependency.
 */
public final class FirebaseIdTokenVerifier implements SocialIdentityVerifier {
  private final String projectId;
  private final Function<String, PublicKey> keys;
  private final Clock clock;

  public FirebaseIdTokenVerifier(String projectId, Function<String, PublicKey> keys, Clock clock) {
    if (projectId == null || !projectId.matches("[a-z0-9][a-z0-9-]{3,61}[a-z0-9]"))
      throw new IllegalArgumentException("Firebase project ID required");
    this.projectId = projectId;
    this.keys = Objects.requireNonNull(keys);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public VerifiedSocialIdentity verify(String token) {
    if (token == null || token.isBlank() || token.length() > 16384) throw invalid();
    try {
      var signed =
          Jwts.parser()
              .clock(() -> Date.from(clock.instant()))
              .requireIssuer("https://securetoken.google.com/" + projectId)
              .keyLocator(
                  new LocatorAdapter<java.security.Key>() {
                    @Override
                    public java.security.Key locate(ProtectedHeader header) {
                      if (!"RS256".equals(header.getAlgorithm()) || header.getKeyId() == null)
                        throw invalid();
                      var key = keys.apply(header.getKeyId());
                      if (key == null) throw invalid();
                      return key;
                    }
                  })
              .build()
              .parseSignedClaims(token);
      var claims = signed.getPayload();
      var now = clock.instant();
      if (claims.getAudience() == null
          || !claims.getAudience().equals(Set.of(projectId))
          || claims.getExpiration() == null
          || !claims.getExpiration().toInstant().isAfter(now)
          || claims.getIssuedAt() == null
          || claims.getIssuedAt().toInstant().isAfter(now)) throw invalid();
      var authTime = claims.get("auth_time", Number.class);
      if (authTime == null
          || authTime.longValue() < 0
          || authTime.longValue() > now.getEpochSecond()) throw invalid();
      var firebase = claims.get("firebase", Map.class);
      if (firebase == null) throw invalid();
      var provider =
          switch (String.valueOf(firebase.get("sign_in_provider"))) {
            case "google.com" -> VerifiedSocialIdentity.Provider.GOOGLE;
            case "facebook.com" -> VerifiedSocialIdentity.Provider.FACEBOOK;
            default -> throw invalid();
          };
      return new VerifiedSocialIdentity(
          claims.getIssuer(),
          claims.getSubject(),
          provider,
          claims.get("email", String.class),
          Boolean.TRUE.equals(claims.get("email_verified", Boolean.class)),
          claims.get("name", String.class));
    } catch (RuntimeException failure) {
      // Do not retain parser errors: third-party exceptions can include token claims.
      throw invalid();
    }
  }

  private static IllegalArgumentException invalid() {
    return new IllegalArgumentException("Invalid social credential");
  }
}
