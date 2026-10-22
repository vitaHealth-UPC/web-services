package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

import com.vitahealth.tata.omissionescalation.application.internal.commandservices.GenerateCaregiverAlertCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.SendReinforcedReminderCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Clock;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Recovers old active source cases without intentions, isolating each command transaction. */
@Component
public class NotificationIntentRecoveryScheduler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(NotificationIntentRecoveryScheduler.class);
  private static final int PAGE_SIZE = 100;
  private final OmissionCaseJpaRepository cases;
  private final SendReinforcedReminderCommandHandler reminders;
  private final GenerateCaregiverAlertCommandHandler alerts;
  private final Clock clock;

  /** Injects scalar selection and the same locked command boundaries used by new source events. */
  public NotificationIntentRecoveryScheduler(
      OmissionCaseJpaRepository cases,
      SendReinforcedReminderCommandHandler reminders,
      GenerateCaregiverAlertCommandHandler alerts,
      Clock clock) {
    this.cases = cases;
    this.reminders = reminders;
    this.alerts = alerts;
    this.clock = clock;
  }

  /**
   * Recovers missing intentions without changing old clinical times or reopening cancelled work.
   */
  @Scheduled(
      fixedDelayString = "${tata.notifications.recovery-delay:30000}",
      initialDelayString = "${tata.notifications.recovery-initial-delay:30000}")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void recover() {
    scan(
        cursor ->
            cases.findMissingReminderIntentIdsAfter(
                cursor,
                OmissionCaseStatus.PENDING,
                NotificationKind.REINFORCED_REMINDER,
                PageRequest.of(0, PAGE_SIZE)),
        id -> reminders.handle(new SendReinforcedReminderCommand(id)));
    scan(
        cursor ->
            cases.findMissingAlertIntentIdsAfter(
                cursor,
                List.of(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED),
                AlertStatus.SENT,
                NotificationKind.CAREGIVER_ALERT,
                PageRequest.of(0, PAGE_SIZE)),
        id -> alerts.handle(new GenerateCaregiverAlertCommand(id, clock.instant())));
  }

  private static void scan(LongFunction<List<Long>> selection, LongConsumer command) {
    long cursor = 0;
    while (true) {
      var page = selection.apply(cursor);
      if (page.isEmpty()) return;
      for (long id : page) {
        if (id <= cursor)
          throw new IllegalStateException("notification recovery cursor did not advance");
        try {
          command.accept(id);
        } catch (RuntimeException failure) {
          LOGGER.warn("Notification recovery failed category=NOTIFICATION_INTENT_RECOVERY_FAILED");
        }
        cursor = id;
      }
      if (page.size() < PAGE_SIZE) return;
    }
  }
}
