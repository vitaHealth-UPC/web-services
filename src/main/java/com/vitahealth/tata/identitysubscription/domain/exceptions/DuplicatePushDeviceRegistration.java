package com.vitahealth.tata.identitysubscription.domain.exceptions;

/**
 * Signals a competing installation or token registration after its write transaction rolls back.
 */
public final class DuplicatePushDeviceRegistration extends RuntimeException {
  /** Exposes a stable conflict without leaking database rows or opaque device addresses. */
  public DuplicatePushDeviceRegistration() {
    super("push device registration conflicts with an existing registration");
  }
}
