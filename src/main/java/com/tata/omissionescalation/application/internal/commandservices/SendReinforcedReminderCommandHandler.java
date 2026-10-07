package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.application.internal.outboundservices.INotificationPort;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendReinforcedReminderCommandHandler {

  private final IOmissionCaseRepository repository;
  private final INotificationPort notificationPort;
  private final INotificationPreferencesPort preferencesPort;
  private final Clock clock;

  @Autowired
  public SendReinforcedReminderCommandHandler(
      IOmissionCaseRepository repository,
      INotificationPort notificationPort,
      INotificationPreferencesPort preferencesPort) {
    this(repository, notificationPort, preferencesPort, Clock.systemUTC());
  }

  SendReinforcedReminderCommandHandler(
      IOmissionCaseRepository repository,
      INotificationPort notificationPort,
      INotificationPreferencesPort preferencesPort,
      Clock clock) {
    this.repository = repository;
    this.notificationPort = notificationPort;
    this.preferencesPort = preferencesPort;
    this.clock = clock;
  }

  /** Sends one reinforced reminder per case, respecting the user's channel and quiet hours. */
  @Transactional
  public void handle(SendReinforcedReminderCommand command) {
    OmissionCase omissionCase = repository.findById(command.omissionCaseId()).orElse(null);
    if (omissionCase == null
        || omissionCase.getStatus() != OmissionCaseStatus.PENDING
        || omissionCase.getReinforcedReminderSentAt() != null) {
      return;
    }
    Instant now = clock.instant();
    NotificationPreferences preferences =
        preferencesPort.getPreferences(omissionCase.getOlderAdultId());
    if (!preferences.pushEnabled() || preferences.isQuietAt(now)) {
      return;
    }
    INotificationPort.NotificationResult result = notificationPort.sendReinforcedReminder(
        omissionCase.getOlderAdultId(),
        omissionCase.getMedicationName(),
        omissionCase.getScheduledAt());
    if (result.delivered()) {
      omissionCase.markReminderSent(now);
      repository.save(omissionCase);
    }
  }
}
