package com.vitahealth.tata.intakeexecution.application.internal.outboundservices;

/** Resolves revocable caller references without carrying credentials through speech processing. */
public interface IntakeConfirmationAccessPort {
  /** Authenticates a credential and returns its stable internal session reference. */
  String authenticate(String credential);

  /** Requires current session eligibility and access to the actual intake owner. */
  void requireAuthorized(String sessionReference, String olderAdultId);
}
