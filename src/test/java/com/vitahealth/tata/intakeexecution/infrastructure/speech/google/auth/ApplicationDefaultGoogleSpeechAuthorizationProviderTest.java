package com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Exercises lazy credential loading and refresh using a simulated SDK source, never real ADC. */
class ApplicationDefaultGoogleSpeechAuthorizationProviderTest {
  private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void loadsOnceRefreshesOnAccessAndPreservesQuotaProject() throws Exception {
    var credentials = mock(GoogleCredentials.class);
    when(credentials.getAccessToken())
        .thenReturn(
            new AccessToken("first-secret", Date.from(NOW.plusSeconds(3600))),
            new AccessToken("renewed-secret", Date.from(NOW.plusSeconds(7200))));
    when(credentials.getQuotaProjectId()).thenReturn("test-project");
    var loads = new AtomicInteger();
    var provider =
        new ApplicationDefaultGoogleSpeechAuthorizationProvider(
            () -> {
              loads.incrementAndGet();
              return credentials;
            },
            CLOCK);
    assertEquals(0, loads.get());
    var first = provider.authorization();
    assertEquals("first-secret", first.accessToken());
    assertEquals("test-project", first.quotaProjectId());
    assertEquals("renewed-secret", provider.authorization().accessToken());
    assertEquals(1, loads.get());
    verify(credentials, times(2)).refreshIfExpired();
    assertFalse(first.toString().contains("first-secret"));
  }

  @Test
  void rejectsExpiredMissingAndBlankTokens() {
    for (var token :
        new AccessToken[] {
          null,
          new AccessToken("expired", Date.from(NOW)),
          new AccessToken("missing-expiry", null),
          new AccessToken(" ", Date.from(NOW.plusSeconds(60)))
        }) {
      var credentials = mock(GoogleCredentials.class);
      when(credentials.getAccessToken()).thenReturn(token);
      var provider =
          new ApplicationDefaultGoogleSpeechAuthorizationProvider(() -> credentials, CLOCK);
      assertThrows(IllegalStateException.class, provider::authorization);
    }
  }

  @Test
  void refreshFailureDoesNotReturnAnOldTokenOrExposeDiagnostics() throws Exception {
    var credentials = mock(GoogleCredentials.class);
    doThrow(new IOException("sensitive credential error")).when(credentials).refreshIfExpired();
    var provider =
        new ApplicationDefaultGoogleSpeechAuthorizationProvider(() -> credentials, CLOCK);
    var failure = assertThrows(IllegalStateException.class, provider::authorization);
    assertEquals("Google Speech credentials are unavailable", failure.getMessage());
  }
}
