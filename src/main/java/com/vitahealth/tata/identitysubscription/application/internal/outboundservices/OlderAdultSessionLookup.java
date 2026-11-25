package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

import java.util.Collection;
import java.util.Set;

/** Reads whether an adult currently holds a live session, without exposing credentials. */
public interface OlderAdultSessionLookup {
  /** True while some unexpired adult session exists for the account. */
  boolean hasActiveSession(String olderAdultId);

  /** The accounts among [olderAdultIds] with a live adult session. */
  Set<String> withActiveSession(Collection<String> olderAdultIds);
}
