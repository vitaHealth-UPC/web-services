package com.vitahealth.tata.identitysubscription.infrastructure.email.encryption;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Encrypts verification codes with versioned external AES keys and authenticated request IDs. */
public final class AesGcmVerificationPayloadCipher implements VerificationPayloadCipher {
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final int MAX_PAYLOAD_LENGTH = 2048;
  private final String activeKeyId;
  private final Map<String, SecretKeySpec> keys;
  private final SecureRandom random = new SecureRandom();

  /** Requires an active key and retains previous keys to decrypt requests during key rotation. */
  public AesGcmVerificationPayloadCipher(String activeKeyId, Map<String, String> base64Keys) {
    if (base64Keys == null || base64Keys.isEmpty()) {
      throw new IllegalArgumentException("Verification encryption keys are required");
    }
    var decoded = new HashMap<String, SecretKeySpec>();
    base64Keys.forEach(
        (id, value) -> {
          requireKeyId(id);
          byte[] bytes;
          try {
            bytes = Base64.getDecoder().decode(value);
          } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Invalid verification encryption key");
          }
          if (bytes.length != 32) {
            throw new IllegalArgumentException("Verification encryption requires a 256-bit key");
          }
          decoded.put(id, new SecretKeySpec(bytes, "AES"));
        });
    requireKeyId(activeKeyId);
    if (!decoded.containsKey(activeKeyId)) {
      throw new IllegalArgumentException("Active verification encryption key is missing");
    }
    this.activeKeyId = activeKeyId;
    this.keys = Map.copyOf(decoded);
  }

  @Override
  public String encrypt(String deliveryId, String code) {
    requireDeliveryId(deliveryId);
    var verifiedCode = new VerificationCode(code);
    byte[] nonce = new byte[NONCE_BYTES];
    random.nextBytes(nonce);
    try {
      var cipher = cipher(Cipher.ENCRYPT_MODE, activeKeyId, nonce, deliveryId);
      byte[] encrypted = cipher.doFinal(verifiedCode.value().getBytes(StandardCharsets.UTF_8));
      var encoder = Base64.getUrlEncoder().withoutPadding();
      return activeKeyId
          + "."
          + encoder.encodeToString(nonce)
          + "."
          + encoder.encodeToString(encrypted);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Verification payload encryption failed");
    }
  }

  @Override
  public String decrypt(String deliveryId, String encryptedPayload) {
    requireDeliveryId(deliveryId);
    if (encryptedPayload == null || encryptedPayload.length() > MAX_PAYLOAD_LENGTH) {
      throw new IllegalArgumentException("Invalid verification payload");
    }
    try {
      String[] parts = encryptedPayload.split("\\.", -1);
      if (parts.length != 3 || !keys.containsKey(parts[0])) {
        throw new IllegalArgumentException("Invalid verification payload");
      }
      byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
      byte[] ciphertext = Base64.getUrlDecoder().decode(parts[2]);
      if (nonce.length != NONCE_BYTES || ciphertext.length != 22) {
        throw new IllegalArgumentException("Invalid verification payload");
      }
      var cipher = cipher(Cipher.DECRYPT_MODE, parts[0], nonce, deliveryId);
      String code = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
      return new VerificationCode(code).value();
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalArgumentException("Verification payload authentication failed");
    }
  }

  private Cipher cipher(int mode, String keyId, byte[] nonce, String deliveryId)
      throws GeneralSecurityException {
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, keys.get(keyId), new GCMParameterSpec(TAG_BITS, nonce));
    cipher.updateAAD((keyId + ":" + deliveryId).getBytes(StandardCharsets.UTF_8));
    return cipher;
  }

  private static void requireDeliveryId(String id) {
    if (id == null || id.isBlank() || id.length() > 128) {
      throw new IllegalArgumentException("A stable verification delivery identifier is required");
    }
  }

  private static void requireKeyId(String id) {
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) {
      throw new IllegalArgumentException("Invalid verification encryption key identifier");
    }
  }
}
