package com.tata.omissionescalation.infrastructure.external.adapters;

import com.tata.omissionescalation.application.internal.outboundservices.INotificationPort;
import com.tata.omissionescalation.infrastructure.external.PushProviderClient;
import com.tata.omissionescalation.infrastructure.external.dto.PushProviderMessage;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer towards the push provider. It translates Tata notifications into the
 * provider message and turns any provider failure into a result instead of an exception, so the
 * business state is never rolled back by a failed delivery.
 */
@Component
public class PushNotificationAdapter implements INotificationPort {

  private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationAdapter.class);

  private final PushProviderClient providerClient;

  public PushNotificationAdapter(PushProviderClient providerClient) {
    this.providerClient = providerClient;
  }

  @Override
  public NotificationResult sendReinforcedReminder(
      String olderAdultId, String medicationName, Instant scheduledAt) {
    return send(new PushProviderMessage(
        "user-" + olderAdultId,
        "Medication reminder",
        "Your " + medicationName + " is still pending. Please confirm it when you take it."));
  }

  @Override
  public NotificationResult sendCaregiverAlert(
      String olderAdultId, String medicationName, Instant scheduledAt) {
    return send(new PushProviderMessage(
        "caregivers-of-" + olderAdultId,
        "Medication not confirmed",
        medicationName + " was not confirmed. Please check how they are doing."));
  }

  private NotificationResult send(PushProviderMessage message) {
    try {
      providerClient.send(message);
      return NotificationResult.success();
    } catch (RuntimeException exception) {
      LOGGER.warn("Push delivery failed for {}: {}", message.recipient(), exception.getMessage());
      return NotificationResult.failure(
          exception.getMessage() == null || exception.getMessage().isBlank()
              ? "push provider failure"
              : exception.getMessage());
    }
  }
}
