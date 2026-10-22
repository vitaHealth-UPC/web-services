package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.infrastructure.hashing.bcrypt.BCryptPasswordHasher;
import org.junit.jupiter.api.Test;

/** Checks UTF-8 boundaries without allowing a credential's suffix to be silently discarded. */
class BCryptPasswordHasherTest {
  private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

  @Test
  void acceptsExactlySeventyTwoUtf8Bytes() {
    var raw = "á".repeat(36);
    var hash = hasher.hash(raw);
    assertThat(hasher.matches(raw, hash)).isTrue();
    assertThat(hasher.matches(raw + "a", hash)).isFalse();
  }

  @Test
  void rejectsOversizedCredentialBeforeEncoding() {
    assertThatThrownBy(() -> hasher.hash("á".repeat(37)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(IllegalArgumentException.class);
    assertThat(hasher.matches(null, "hash")).isFalse();
  }
}
