package com.vitahealth.tata.omissionescalation.application.internal.outboundservices;

import com.vitahealth.tata.omissionescalation.application.result.NotificationCaregiverPage;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDeviceAddress;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDevicePage;
import java.util.Optional;

/**
 * Resolves authorized logical recipients and concrete devices without exposing neighboring models.
 */
public interface NotificationRecipientPort {
  /** Scans currently consented caregiver identities for the adult. */
  NotificationCaregiverPage findCaregivers(String olderAdultId, String afterId, int limit);

  /** Resolves adult devices through active adult-role sessions. */
  NotificationDevicePage findOlderAdultDevices(String olderAdultId, String afterId, int limit);

  /** Rechecks the member pair before returning devices belonging to the caregiver. */
  NotificationDevicePage findCaregiverDevices(
      String olderAdultId, String caregiverId, String afterId, int limit);

  /** Revalidates an adult address without allowing a different session to receive queued work. */
  Optional<NotificationDeviceAddress> findOlderAdultDevice(
      String olderAdultId, String installationId, String sessionId);

  /**
   * Revalidates both caregiver consent and the original device session immediately before dispatch.
   */
  Optional<NotificationDeviceAddress> findCaregiverDevice(
      String olderAdultId, String caregiverId, String installationId, String sessionId);
}
