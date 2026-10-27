package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

/** Revokes password-based account access inside the password-mutation transaction. */
public interface RevokeAccountSessionsPort {
  void revokeCaregiverSessions(String accountId);
}
