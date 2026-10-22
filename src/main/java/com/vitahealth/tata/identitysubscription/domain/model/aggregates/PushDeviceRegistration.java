package com.vitahealth.tata.identitysubscription.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One installation's push address owned by a revocable session, independent of Firebase transport.
 */
public record PushDeviceRegistration(
    String installationId, String sessionId, String token, Instant createdAt, Instant updatedAt) {
  /** Requires explicit ownership, a usable opaque address and coherent write evidence. */
  public PushDeviceRegistration {
    if (installationId == null
        || !UUID.fromString(installationId).toString().equals(installationId))
      throw new IllegalArgumentException("installation id must be a canonical UUID");
    if (sessionId == null || sessionId.isBlank())
      throw new IllegalArgumentException("session id is required");
    if (token == null
        || token.isBlank()
        || token.length() > 4096
        || token
            .codePoints()
            .anyMatch(value -> Character.isWhitespace(value) || Character.isISOControl(value)))
      throw new IllegalArgumentException("push token is invalid");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    if (updatedAt.isBefore(createdAt))
      throw new IllegalArgumentException("device update precedes registration");
  }

  /** Creates a registration whose first write uses the caller's explicit application time. */
  public static PushDeviceRegistration register(
      String installationId, String sessionId, String token, Instant now) {
    return new PushDeviceRegistration(installationId, sessionId, token, now, now);
  }

  /** Rotates the opaque address while retaining original ownership and creation evidence. */
  public PushDeviceRegistration rotate(String replacement, Instant now) {
    if (now == null || now.isBefore(updatedAt))
      throw new IllegalArgumentException("device rotation time is invalid");
    return new PushDeviceRegistration(installationId, sessionId, replacement, createdAt, now);
  }

  /** Prevents tokens and session references from appearing in logs through record formatting. */
  @Override
  public String toString() {
    return "PushDeviceRegistration[redacted]";
  }
}
