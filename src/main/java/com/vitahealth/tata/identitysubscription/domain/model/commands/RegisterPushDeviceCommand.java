package com.vitahealth.tata.identitysubscription.domain.model.commands;

/**
 * Requests installation address registration; ownership comes exclusively from verified identity.
 */
public record RegisterPushDeviceCommand(String installationId, String token) {
  /** Keeps opaque device tokens out of automatic command diagnostics. */
  @Override
  public String toString() {
    return "RegisterPushDeviceCommand[redacted]";
  }
}
