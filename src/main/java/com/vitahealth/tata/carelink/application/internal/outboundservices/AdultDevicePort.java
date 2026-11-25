package com.vitahealth.tata.carelink.application.internal.outboundservices;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Reads the adult's credential state through the Identity public boundary. */
public interface AdultDevicePort {
  /** True when the adult already created a PIN. */
  boolean hasPin(String olderAdultId);

  /** True when some device holds an unexpired adult session. */
  boolean hasActiveSession(String olderAdultId);

  /** Display name of a caregiver account, when it exists. */
  Optional<String> caregiverName(String caregiverId);

  /** Removes the adult's PIN so no device can sign in with it any more. */
  void removePin(String olderAdultId);

  /** The adults among [olderAdultIds] that hold a live session, read in one batch. */
  Set<String> withActiveSession(Collection<String> olderAdultIds);
}
