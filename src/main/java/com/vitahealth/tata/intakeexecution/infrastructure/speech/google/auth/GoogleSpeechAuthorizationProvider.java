package com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth;

/** Supplies renewable provider credentials independently of the HTTP recognition protocol. */
@FunctionalInterface
public interface GoogleSpeechAuthorizationProvider {
  /** Returns a currently valid OAuth identity or fails without issuing an anonymous request. */
  GoogleSpeechAuthorization authorization();
}
