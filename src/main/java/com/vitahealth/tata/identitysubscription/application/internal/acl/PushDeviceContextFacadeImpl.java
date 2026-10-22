package com.vitahealth.tata.identitysubscription.application.internal.acl;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.queryservices.PushDeviceQueryService;
import com.vitahealth.tata.identitysubscription.application.result.PushDeviceAddressResult;
import com.vitahealth.tata.identitysubscription.application.result.PushDevicePageResult;
import com.vitahealth.tata.identitysubscription.interfaces.acl.PushDeviceContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Fixes the requested role at the public context boundary rather than accepting arbitrary scopes.
 */
@Service
public class PushDeviceContextFacadeImpl implements PushDeviceContextFacade {
  private final PushDeviceQueryService queries;

  /** Injects Identity-owned authorized device queries. */
  public PushDeviceContextFacadeImpl(PushDeviceQueryService queries) {
    this.queries = queries;
  }

  /**
   * Resolves active caregiver addresses without exposing Identity's internal role representation.
   */
  @Override
  public PushDevicePageResult findCaregiverDevices(String caregiverId, String afterId, int limit) {
    return queries.findActiveDevices(caregiverId, Role.CAREGIVER, afterId, limit);
  }

  /**
   * Resolves adult addresses only while their original consent-scoped sessions remain authorized.
   */
  @Override
  public PushDevicePageResult findOlderAdultDevices(
      String olderAdultId, String afterId, int limit) {
    return queries.findActiveDevices(olderAdultId, Role.OLDER_ADULT, afterId, limit);
  }

  /** Rechecks caregiver identity and the original device session before dispatch. */
  @Override
  public Optional<PushDeviceAddressResult> findCaregiverDevice(
      String installationId, String sessionId, String caregiverId) {
    return queries.findActiveDevice(installationId, sessionId, caregiverId, Role.CAREGIVER);
  }

  /** Rechecks the original adult session and its live consent scope before dispatch. */
  @Override
  public Optional<PushDeviceAddressResult> findOlderAdultDevice(
      String installationId, String sessionId, String olderAdultId) {
    return queries.findActiveDevice(installationId, sessionId, olderAdultId, Role.OLDER_ADULT);
  }
}
