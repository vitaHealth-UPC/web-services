package com.vitahealth.tata.identitysubscription.application.result;

/** Internal dispatch address with revocable ownership; never exposed as an HTTP resource. */
public record PushDeviceAddressResult(String installationId, String sessionId, String token) {
  /** Keeps device addresses and session references out of automatic diagnostic formatting. */
  @Override
  public String toString() {
    return "PushDeviceAddressResult[redacted]";
  }
}
