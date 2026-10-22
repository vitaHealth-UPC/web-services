package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Lazily loads scoped Google ADC and renews the OAuth token used by FCM HTTP v1. */
public final class FirebaseAccessTokenProvider implements Supplier<String> {
  private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
  private final Supplier<GoogleCredentials> loader;
  private final Clock clock;
  private GoogleCredentials credentials;

  /**
   * Uses deployment-provided credentials without reading a secret file during application startup.
   */
  public FirebaseAccessTokenProvider(Clock clock) {
    this(
        () -> {
          try {
            return GoogleCredentials.getApplicationDefault().createScoped(List.of(SCOPE));
          } catch (IOException failure) {
            throw unavailable();
          }
        },
        clock);
  }

  /** Injects credential loading and time for isolated tests of expiration and renewal. */
  FirebaseAccessTokenProvider(Supplier<GoogleCredentials> loader, Clock clock) {
    this.loader = Objects.requireNonNull(loader, "loader");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Returns a nonexpired token, serializing credential initialization and SDK refresh. */
  @Override
  public synchronized String get() {
    try {
      if (credentials == null) credentials = Objects.requireNonNull(loader.get());
      credentials.refreshIfExpired();
      var token = credentials.getAccessToken();
      if (token == null
          || token.getTokenValue() == null
          || token.getTokenValue().isBlank()
          || token.getExpirationTime() == null
          || !token.getExpirationTime().toInstant().isAfter(clock.instant())) throw unavailable();
      return token.getTokenValue();
    } catch (IOException | RuntimeException failure) {
      throw unavailable();
    }
  }

  private static IllegalStateException unavailable() {
    return new IllegalStateException("Firebase credentials are unavailable");
  }
}
