package com.vitahealth.tata.identitysubscription.interfaces.acl;

import com.vitahealth.tata.identitysubscription.application.result.PushDeviceAddressResult;
import com.vitahealth.tata.identitysubscription.application.result.PushDevicePageResult;
import java.util.Optional;

/** Internal authorized-address boundary for notification dispatch; no REST token listing exists. */
public interface PushDeviceContextFacade {
  /** Reads one caregiver device page with fresh account and session authorization. */
  PushDevicePageResult findCaregiverDevices(String caregiverId, String afterId, int limit);

  /** Reads one adult device page with fresh relationship consent and session authorization. */
  PushDevicePageResult findOlderAdultDevices(String olderAdultId, String afterId, int limit);

  /** Revalidates one caregiver installation against its originally selected session. */
  Optional<PushDeviceAddressResult> findCaregiverDevice(
      String installationId, String sessionId, String caregiverId);

  /** Revalidates one adult installation against its original consent-scoped session. */
  Optional<PushDeviceAddressResult> findOlderAdultDevice(
      String installationId, String sessionId, String olderAdultId);
}
