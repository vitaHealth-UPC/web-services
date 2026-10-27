package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerifiedSocialIdentity;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.firebase.FirebaseIdTokenVerifier;
import io.jsonwebtoken.*;
import java.security.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;

class FirebaseIdTokenVerifierTest {
  private final Instant now = Instant.parse("2026-10-26T12:00:00Z");
  private final KeyPair key = Jwts.SIG.RS256.keyPair().build();

  private JwtBuilder token() {
    return Jwts.builder()
        .header()
        .keyId("trusted")
        .and()
        .issuer("https://securetoken.google.com/tata-project")
        .subject("firebase-uid")
        .audience()
        .add("tata-project")
        .and()
        .issuedAt(Date.from(now.minusSeconds(10)))
        .expiration(Date.from(now.plusSeconds(300)))
        .claim("auth_time", now.minusSeconds(20).getEpochSecond())
        .claim("firebase", Map.of("sign_in_provider", "google.com"))
        .claim("email", "user@example.com")
        .claim("email_verified", true);
  }

  private FirebaseIdTokenVerifier verifier() {
    return new FirebaseIdTokenVerifier(
        "tata-project",
        kid -> kid.equals("trusted") ? key.getPublic() : null,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void acceptsOnlySignedGoogleOrFacebookIdentityAndRedactsProfile() {
    var identity = verifier().verify(token().signWith(key.getPrivate(), Jwts.SIG.RS256).compact());
    assertEquals("firebase-uid", identity.subject());
    assertEquals(VerifiedSocialIdentity.Provider.GOOGLE, identity.provider());
    assertFalse(identity.toString().contains("user@example.com"));
    assertEquals(
        VerifiedSocialIdentity.Provider.FACEBOOK,
        verifier()
            .verify(
                token()
                    .claim("firebase", Map.of("sign_in_provider", "facebook.com"))
                    .signWith(key.getPrivate(), Jwts.SIG.RS256)
                    .compact())
            .provider());
  }

  @Test
  void rejectsExpiredForeignUnsignedAndUnsupportedProviderEvidence() {
    var foreign = Jwts.SIG.RS256.keyPair().build();
    var invalid =
        List.of(
            token().expiration(Date.from(now)).signWith(key.getPrivate(), Jwts.SIG.RS256).compact(),
            token()
                .issuer("https://securetoken.google.com/other-project")
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact(),
            token()
                .claim("aud", "other-project")
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact(),
            token()
                .issuedAt(Date.from(now.plusSeconds(1)))
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact(),
            token()
                .claim("auth_time", now.plusSeconds(1).getEpochSecond())
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact(),
            token()
                .claim("firebase", Map.of("sign_in_provider", "password"))
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact(),
            token().signWith(foreign.getPrivate(), Jwts.SIG.RS256).compact(),
            token().compact());
    for (var value : invalid) {
      var error = assertThrows(IllegalArgumentException.class, () -> verifier().verify(value));
      assertEquals("Invalid social credential", error.getMessage());
      assertNull(error.getCause());
    }
  }
}
