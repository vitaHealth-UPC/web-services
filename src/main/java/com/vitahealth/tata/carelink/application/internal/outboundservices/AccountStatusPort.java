package com.vitahealth.tata.carelink.application.internal.outboundservices;

/** Reads current caregiver account eligibility through the Identity public boundary. */
public interface AccountStatusPort {
  /** Returns false for absent or disabled accounts; technical lookup failures propagate. */
  boolean isEnabled(String accountId);
}
