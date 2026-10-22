package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

/** Sends an eligible queued verification message through an external provider. */
public interface VerificationEmailProvider {
  /** Returns the provider acceptance identifier; this does not establish inbox delivery. */
  String send(VerificationDeliveryRequest request);
}
