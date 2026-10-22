package com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import java.time.Instant;
import java.util.List;

/**
 * Selects bounded device candidates; returned session evidence still requires live authorization.
 */
public interface PushDeviceCandidateLookup {
  /**
   * Scans one ordered installation page scoped by persisted subject, role and session expiration.
   */
  List<PushDeviceRegistration> findCandidates(
      String subjectId, Role role, String afterId, int limit, Instant now);
}
