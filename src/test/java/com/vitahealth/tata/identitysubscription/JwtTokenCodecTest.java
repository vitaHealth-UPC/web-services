package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt.JwtTokenCodec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Exercises signature, expiry and restricted session identity at the JWT boundary. */
class JwtTokenCodecTest {
  private final Instant now = Instant.parse("2026-10-11T12:00:00Z");
  private final String key = Base64.getEncoder().encodeToString(new byte[32]);
  private final JwtTokenCodec codec = new JwtTokenCodec(key, Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void retainsRestrictedIdentityAndSessionReference() {
    var identity = new AuthenticatedSession("adult", Role.LINK_SETUP, now.plusSeconds(900), "link");
    var token = codec.encode("session", identity);
    var verified = codec.decode(token).orElseThrow();
    assertThat(verified.sessionId()).isEqualTo("session");
    assertThat(verified.identity()).isEqualTo(identity);
  }

  @Test
  void rejectsExpiredAndMalformedCredentials() {
    var identity = new AuthenticatedSession("owner", Role.CAREGIVER, now.plusSeconds(60), null);
    var token = codec.encode("session", identity);
    var later = new JwtTokenCodec(key, Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC));
    assertThat(later.decode(token)).isEmpty();
    assertThat(codec.decode("not.a.jwt")).isEmpty();
    assertThat(codec.decode(null)).isEmpty();
  }

  @Test
  void rejectsSignatureMadeWithAnotherKey() {
    var bytes = new byte[32];
    bytes[0] = 1;
    var other =
        new JwtTokenCodec(
            Base64.getEncoder().encodeToString(bytes), Clock.fixed(now, ZoneOffset.UTC));
    var token =
        other.encode(
            "session",
            new AuthenticatedSession("owner", Role.CAREGIVER, now.plusSeconds(60), null));
    assertThat(codec.decode(token)).isEmpty();
  }
}
