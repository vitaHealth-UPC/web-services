package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Queues a durable reminder while delivery and current preferences are handled after commit. */
@Service
public class SendReinforcedReminderCommandHandler {
  private final IOmissionCaseRepository repository;
  private final NotificationIntentStore intentions;
  private final Clock clock;

  /**
   * Injects reserved source storage and atomic intention persistence, without a network provider.
   */
  public SendReinforcedReminderCommandHandler(
      IOmissionCaseRepository repository, NotificationIntentStore intentions, Clock clock) {
    this.repository = repository;
    this.intentions = intentions;
    this.clock = clock;
  }

  /**
   * Retains unsent work even during quiet hours; dispatcher policy checks determine actual sending.
   */
  @Transactional
  public void handle(SendReinforcedReminderCommand command) {
    var source = repository.findByIdForUpdate(command.omissionCaseId()).orElse(null);
    if (source == null
        || source.getStatus() != OmissionCaseStatus.PENDING
        || source.getReinforcedReminderSentAt() != null) return;
    intentions.enqueue(
        new NotificationIntentRequest(
            command.omissionCaseId(), NotificationKind.REINFORCED_REMINDER, clock.instant()));
  }
}
