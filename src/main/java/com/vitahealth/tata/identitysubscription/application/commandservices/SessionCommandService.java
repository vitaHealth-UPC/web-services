package com.vitahealth.tata.identitysubscription.application.commandservices;

/** Defines session lifecycle writes for REST and other inbound adapters. */
public interface SessionCommandService {
  /** Invalidates the supplied bearer token without disclosing whether it existed. */
  void revoke(String token);
}
