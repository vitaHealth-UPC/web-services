package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies credential loading without accessing ADC files or Google token endpoints. */
class FirebaseAccessTokenProviderTest {
  @Test
  void loadingIsLazyAndSuccessfulCredentialsAreReused() {
    var loads = new AtomicInteger();
    var now = Instant.now();
    var provider =
        new FirebaseAccessTokenProvider(
            () -> {
              loads.incrementAndGet();
              return GoogleCredentials.create(
                  new AccessToken("test-oauth", Date.from(now.plusSeconds(3600))));
            },
            Clock.fixed(now, ZoneOffset.UTC));
    assertThat(loads).hasValue(0);
    assertThat(provider.get()).isEqualTo("test-oauth");
    assertThat(provider.get()).isEqualTo("test-oauth");
    assertThat(loads).hasValue(1);
  }

  @Test
  void failedCredentialLoadingCanRecoverWithoutLeakingItsCause() {
    var loads = new AtomicInteger();
    var now = Instant.now();
    var provider =
        new FirebaseAccessTokenProvider(
            () -> {
              if (loads.incrementAndGet() == 1)
                throw new IllegalStateException("private-key-path secret");
              return GoogleCredentials.create(
                  new AccessToken("test-oauth", Date.from(now.plusSeconds(3600))));
            },
            Clock.fixed(now, ZoneOffset.UTC));
    assertThatThrownBy(provider::get)
        .hasMessage("Firebase credentials are unavailable")
        .hasNoCause();
    assertThat(provider.get()).isEqualTo("test-oauth");
  }

  @Test
  void applicationClockRejectsExpiredTokenEvenWhenSdkClockHasNotExpiredIt() {
    var now = Instant.now();
    var provider =
        new FirebaseAccessTokenProvider(
            () ->
                GoogleCredentials.create(
                    new AccessToken("test-oauth", Date.from(now.plusSeconds(3600)))),
            Clock.fixed(now.plusSeconds(7200), ZoneOffset.UTC));
    assertThatThrownBy(provider::get)
        .hasMessage("Firebase credentials are unavailable")
        .hasNoCause();
  }
}
