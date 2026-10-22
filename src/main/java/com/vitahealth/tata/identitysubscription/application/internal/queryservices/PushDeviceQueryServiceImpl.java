package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.PushDeviceCandidateLookup;
import com.vitahealth.tata.identitysubscription.application.queryservices.PushDeviceQueryService;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.identitysubscription.application.result.PushDeviceAddressResult;
import com.vitahealth.tata.identitysubscription.application.result.PushDevicePageResult;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Separates efficient candidate selection from fresh account and consent authorization. */
@Service
public class PushDeviceQueryServiceImpl implements PushDeviceQueryService {
  private final PushDeviceCandidateLookup candidates;
  private final SessionRegistrationQueryService sessions;
  private final Clock clock;
  private final PushDeviceRegistrationRepository devices;

  /** Injects bounded storage selection, live session authorization and application time. */
  public PushDeviceQueryServiceImpl(
      PushDeviceCandidateLookup candidates,
      SessionRegistrationQueryService sessions,
      Clock clock,
      PushDeviceRegistrationRepository devices) {
    this.candidates = candidates;
    this.sessions = sessions;
    this.clock = clock;
    this.devices = devices;
  }

  /** Advances the scan cursor even when a full candidate page loses authorization. */
  @Override
  public PushDevicePageResult findActiveDevices(
      String subjectId, Role role, String afterId, int limit) {
    if (subjectId == null
        || subjectId.isBlank()
        || role == null
        || role == Role.LINK_SETUP
        || afterId == null
        || limit < 1
        || limit > 100) throw new IllegalArgumentException("device page scope is invalid");
    var page = candidates.findCandidates(subjectId, role, afterId, limit, clock.instant());
    var addresses = new ArrayList<PushDeviceAddressResult>();
    for (var device : page) {
      var active = sessions.findActiveRegistration(device.sessionId());
      if (active.isPresent()) {
        var identity = active.orElseThrow().identity();
        if (identity.subjectId().equals(subjectId) && identity.role() == role)
          addresses.add(
              new PushDeviceAddressResult(
                  device.installationId(), device.sessionId(), device.token()));
      }
    }
    var cursor = page.size() == limit ? page.getLast().installationId() : null;
    return new PushDevicePageResult(addresses, cursor);
  }

  /**
   * Never redirects queued work to a replacement session; same-session rotation uses its latest
   * token.
   */
  @Override
  public Optional<PushDeviceAddressResult> findActiveDevice(
      String installationId, String sessionId, String subjectId, Role role) {
    if (installationId == null
        || installationId.isBlank()
        || sessionId == null
        || sessionId.isBlank()
        || subjectId == null
        || subjectId.isBlank()
        || role == null
        || role == Role.LINK_SETUP)
      throw new IllegalArgumentException("device delivery scope is invalid");
    return devices
        .findByInstallationId(installationId)
        .filter(device -> device.sessionId().equals(sessionId))
        .flatMap(
            device ->
                sessions
                    .findActiveRegistration(sessionId)
                    .filter(
                        registration ->
                            registration.identity().subjectId().equals(subjectId)
                                && registration.identity().role() == role)
                    .map(
                        registration ->
                            new PushDeviceAddressResult(
                                device.installationId(), device.sessionId(), device.token())));
  }
}
