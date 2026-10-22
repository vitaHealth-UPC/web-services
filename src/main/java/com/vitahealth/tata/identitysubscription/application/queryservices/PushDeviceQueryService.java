package com.vitahealth.tata.identitysubscription.application.queryservices;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.result.PushDeviceAddressResult;
import com.vitahealth.tata.identitysubscription.application.result.PushDevicePageResult;
import java.util.Optional;

/** Reads authorized device addresses for internal cross-context dispatch only. */
public interface PushDeviceQueryService {
  /** Returns a bounded page while rechecking every candidate's live session ownership. */
  PushDevicePageResult findActiveDevices(String subjectId, Role role, String afterId, int limit);

  /** Resolves the current address only while its original session, subject and role still match. */
  Optional<PushDeviceAddressResult> findActiveDevice(
      String installationId, String sessionId, String subjectId, Role role);
}
