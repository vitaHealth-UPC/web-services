package com.vitahealth.tata.carelink.application.internal.outboundservices;

import java.time.Instant;

/** Coordinates active-code allocation while retaining the caller's complete command transaction. */
public interface LinkingCodeAllocationPort {
  /** Reserves issuance in the shared code namespace until the current transaction finishes. */
  void reserveNamespace();

  /** Releases expired reservations without changing historical relationship or code evidence. */
  void releaseExpiredReservations(Instant now);
}
