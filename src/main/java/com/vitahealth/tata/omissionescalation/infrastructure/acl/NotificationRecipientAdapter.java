package com.vitahealth.tata.omissionescalation.infrastructure.acl;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.result.PushDevicePageResult;
import com.vitahealth.tata.identitysubscription.interfaces.acl.PushDeviceContextFacade;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationRecipientPort;
import com.vitahealth.tata.omissionescalation.application.result.NotificationCaregiverPage;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDeviceAddress;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDevicePage;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Translates public Care Link and Identity ACL evidence into Omission's dispatch contracts. */
@Component
public class NotificationRecipientAdapter implements NotificationRecipientPort {
  private final CareLinkContextFacade relationships;
  private final PushDeviceContextFacade devices;

  /**
   * Injects context boundaries without querying another context's persistence or domain objects.
   */
  public NotificationRecipientAdapter(
      CareLinkContextFacade relationships, PushDeviceContextFacade devices) {
    this.relationships = relationships;
    this.devices = devices;
  }

  /** Returns caregiver identities without leaking linking codes or consent payloads. */
  @Override
  public NotificationCaregiverPage findCaregivers(String olderAdultId, String afterId, int limit) {
    var page = relationships.findAuthorizedCaregivers(olderAdultId, afterId, limit);
    return new NotificationCaregiverPage(page.caregiverIds(), page.nextCursor());
  }

  /** Resolves only authenticated adult addresses through Identity's role-specific boundary. */
  @Override
  public NotificationDevicePage findOlderAdultDevices(
      String olderAdultId, String afterId, int limit) {
    return toPage(devices.findOlderAdultDevices(olderAdultId, afterId, limit));
  }

  /** Prevents address lookup for an unrelated or withdrawn caregiver/adult pair. */
  @Override
  public NotificationDevicePage findCaregiverDevices(
      String olderAdultId, String caregiverId, String afterId, int limit) {
    if (!relationships.isAuthorized(caregiverId, olderAdultId))
      return new NotificationDevicePage(List.of(), null);
    return toPage(devices.findCaregiverDevices(caregiverId, afterId, limit));
  }

  private static NotificationDevicePage toPage(PushDevicePageResult page) {
    return new NotificationDevicePage(
        page.devices().stream()
            .map(
                device ->
                    new NotificationDeviceAddress(
                        device.installationId(), device.sessionId(), device.token()))
            .toList(),
        page.nextCursor());
  }

  /** Translates a currently authorized original adult-session address. */
  @Override
  public Optional<NotificationDeviceAddress> findOlderAdultDevice(
      String olderAdultId, String installationId, String sessionId) {
    return devices
        .findOlderAdultDevice(installationId, sessionId, olderAdultId)
        .map(
            device ->
                new NotificationDeviceAddress(
                    device.installationId(), device.sessionId(), device.token()));
  }

  /** Checks current consent before resolving the caregiver's original-session installation. */
  @Override
  public Optional<NotificationDeviceAddress> findCaregiverDevice(
      String olderAdultId, String caregiverId, String installationId, String sessionId) {
    if (!relationships.isAuthorized(caregiverId, olderAdultId)) return Optional.empty();
    return devices
        .findCaregiverDevice(installationId, sessionId, caregiverId)
        .map(
            device ->
                new NotificationDeviceAddress(
                    device.installationId(), device.sessionId(), device.token()));
  }
}
