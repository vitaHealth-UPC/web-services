package com.vitahealth.tata.identitysubscription.infrastructure.email.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies authentication, nonce uniqueness, and key rotation for durable verification codes. */
class AesGcmVerificationPayloadCipherTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void roundTripsCodeWithFreshNonce() {
    var cipher = new AesGcmVerificationPayloadCipher("v1", Map.of("v1", KEY));
    String first = cipher.encrypt("request-1", "012345");
    String second = cipher.encrypt("request-1", "012345");
    assertNotEquals(first, second);
    assertEquals("012345", cipher.decrypt("request-1", first));
    assertEquals("012345", cipher.decrypt("request-1", second));
  }

  @Test
  void rejectsDifferentRequestAndTampering() {
    var cipher = new AesGcmVerificationPayloadCipher("v1", Map.of("v1", KEY));
    String payload = cipher.encrypt("request-1", "123456");
    assertThrows(IllegalArgumentException.class, () -> cipher.decrypt("request-2", payload));
    String[] parts = payload.split("\\.");
    byte[] bytes = Base64.getUrlDecoder().decode(parts[2]);
    bytes[0] ^= 1;
    String changed =
        parts[0]
            + "."
            + parts[1]
            + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    assertThrows(IllegalArgumentException.class, () -> cipher.decrypt("request-1", changed));
    assertThrows(IllegalArgumentException.class, () -> cipher.decrypt("request-1", "bad"));
  }

  @Test
  void retainsOldKeysDuringRotationAndRejectsRemovedKeys() {
    var old = new AesGcmVerificationPayloadCipher("v1", Map.of("v1", KEY));
    String payload = old.encrypt("request-1", "123456");
    byte[] nextBytes = new byte[32];
    nextBytes[0] = 1;
    String next = Base64.getEncoder().encodeToString(nextBytes);
    var rotated = new AesGcmVerificationPayloadCipher("v2", Map.of("v1", KEY, "v2", next));
    assertEquals("123456", rotated.decrypt("request-1", payload));
    var removed = new AesGcmVerificationPayloadCipher("v2", Map.of("v2", next));
    assertThrows(IllegalArgumentException.class, () -> removed.decrypt("request-1", payload));
  }

  @Test
  void rejectsMissingWeakKeysAndInvalidCodes() {
    assertThrows(
        IllegalArgumentException.class, () -> new AesGcmVerificationPayloadCipher("v1", Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AesGcmVerificationPayloadCipher("v1", Map.of("v1", "invalid")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AesGcmVerificationPayloadCipher("v2", Map.of("v1", KEY)));
    var cipher = new AesGcmVerificationPayloadCipher("v1", Map.of("v1", KEY));
    assertThrows(IllegalArgumentException.class, () -> cipher.encrypt("request-1", "12345"));
    assertThrows(IllegalArgumentException.class, () -> cipher.encrypt("", "123456"));
  }
}
