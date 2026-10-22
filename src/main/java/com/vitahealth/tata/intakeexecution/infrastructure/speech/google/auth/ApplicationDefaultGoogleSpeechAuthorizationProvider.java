package com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

/** Lazily loads Google ADC once and lets the Google Auth SDK refresh each expiring access token. */
public class ApplicationDefaultGoogleSpeechAuthorizationProvider
    implements GoogleSpeechAuthorizationProvider {
  private final Supplier<GoogleCredentials> loader;
  private final Clock clock;
  private GoogleCredentials credentials;

  /** Injects the ADC loader and application time without resolving credentials during startup. */
  public ApplicationDefaultGoogleSpeechAuthorizationProvider(
      Supplier<GoogleCredentials> loader, Clock clock) {
    this.loader = Objects.requireNonNull(loader, "loader");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Serializes initial loading and refresh, preserving ADC quota-project attribution. */
  @Override
  public synchronized GoogleSpeechAuthorization authorization() {
    if (credentials == null)
      credentials = Objects.requireNonNull(loader.get(), "Google credentials");
    try {
      credentials.refreshIfExpired();
    } catch (IOException failure) {
      throw new IllegalStateException("Google Speech credentials are unavailable");
    }
    var token = credentials.getAccessToken();
    if (token == null
        || token.getTokenValue() == null
        || token.getTokenValue().isBlank()
        || token.getExpirationTime() == null
        || !token.getExpirationTime().toInstant().isAfter(clock.instant()))
      throw new IllegalStateException("Google Speech credentials are expired or incomplete");
    return new GoogleSpeechAuthorization(token.getTokenValue(), credentials.getQuotaProjectId());
  }
}
