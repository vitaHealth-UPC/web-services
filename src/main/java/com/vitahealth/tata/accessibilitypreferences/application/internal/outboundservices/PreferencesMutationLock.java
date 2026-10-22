package com.vitahealth.tata.accessibilitypreferences.application.internal.outboundservices;

/** Serializes preference decisions for one logical user, including absent initial state. */
public interface PreferencesMutationLock {
  /** Requires a caller transaction and retains the reservation until it commits or rolls back. */
  void reserve(String userId);
}
