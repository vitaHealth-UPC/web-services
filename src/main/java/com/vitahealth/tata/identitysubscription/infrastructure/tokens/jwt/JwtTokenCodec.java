package com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * Signs and verifies session claims using the professor's JJWT approach, adapted under MIT.
 *
 * <p>A valid signature is only one check: the session service must also verify persisted
 * revocation, account status and current consent before returning an authenticated identity.
 */
public final class JwtTokenCodec {
  private static final String ISSUER = "tata-web-services";
  private final SecretKey key;
  private final Clock clock;

  /** Requires a base64 encoded 256-bit key supplied externally and an observable time source. */
  public JwtTokenCodec(String base64Key, Clock clock) {
    var bytes = Base64.getDecoder().decode(Objects.requireNonNull(base64Key, "base64Key"));
    if (bytes.length != 32) {
      throw new IllegalArgumentException("JWT HS256 key must contain exactly 32 bytes");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Signs a unique session reference with its subject, role, expiry and optional care link. */
  public String encode(String sessionId, AuthenticatedSession identity) {
    Objects.requireNonNull(identity, "identity");
    if (sessionId == null
        || sessionId.isBlank()
        || identity.subjectId() == null
        || identity.subjectId().isBlank()
        || identity.role() == null
        || identity.expiresAt() == null
        || !identity.expiresAt().isAfter(clock.instant())) {
      throw new IllegalArgumentException("valid session identity and future expiry are required");
    }
    if (identity.role() != Role.CAREGIVER
        && (identity.careLinkId() == null || identity.careLinkId().isBlank())) {
      throw new IllegalArgumentException("adult and onboarding sessions require a care link");
    }
    return Jwts.builder()
        .issuer(ISSUER)
        .id(sessionId)
        .subject(identity.subjectId())
        .issuedAt(Date.from(clock.instant()))
        .expiration(Date.from(identity.expiresAt()))
        .claim("role", identity.role().name())
        .claim("careLinkId", identity.careLinkId())
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /** Rejects invalid, unsigned, expired or incomplete claims without logging credentials. */
  public Optional<VerifiedClaims> decode(String token) {
    if (token == null || token.isBlank() || token.length() > 4096) return Optional.empty();
    try {
      var signed =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(ISSUER)
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token);
      if (!"HS256".equals(signed.getHeader().getAlgorithm())) return Optional.empty();
      var claims = signed.getPayload();
      var roleName = claims.get("role", String.class);
      if (roleName == null) return Optional.empty();
      var role = Role.valueOf(roleName);
      var linkId = claims.get("careLinkId", String.class);
      if (claims.getId() == null
          || claims.getId().isBlank()
          || claims.getSubject() == null
          || claims.getSubject().isBlank()
          || claims.getExpiration() == null
          || claims.getIssuedAt() == null
          || claims.getIssuedAt().toInstant().isAfter(clock.instant())
          || !claims.getExpiration().toInstant().isAfter(clock.instant())) return Optional.empty();
      if (role != Role.CAREGIVER && (linkId == null || linkId.isBlank())) return Optional.empty();
      return Optional.of(
          new VerifiedClaims(
              claims.getId(),
              new AuthenticatedSession(
                  claims.getSubject(), role, claims.getExpiration().toInstant(), linkId)));
    } catch (JwtException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /** Cryptographically verified claims that still require current database authorization checks. */
  public record VerifiedClaims(String sessionId, AuthenticatedSession identity) {}
}
