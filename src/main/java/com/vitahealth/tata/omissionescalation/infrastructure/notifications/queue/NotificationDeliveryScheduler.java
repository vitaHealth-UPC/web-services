package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import java.time.Clock;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates bounded durable notification work without retaining database transactions. */
@Component
public class NotificationDeliveryScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryScheduler.class);
  private static final int PAGE_SIZE = 100;
  private final NotificationIntentJpaRepository intents;
  private final NotificationDeviceDeliveryJpaRepository deliveries;
  private final NotificationFanoutPageProcessor fanout;
  private final NotificationDeviceDispatcher dispatcher;
  private final NotificationCompletionProcessor completion;
  private final Clock clock;

  /** Injects independent processors so each failed record rolls back only its own work. */
  public NotificationDeliveryScheduler(
      NotificationIntentJpaRepository intents,
      NotificationDeviceDeliveryJpaRepository deliveries,
      NotificationFanoutPageProcessor fanout,
      NotificationDeviceDispatcher dispatcher,
      NotificationCompletionProcessor completion,
      Clock clock) {
    this.intents = intents;
    this.deliveries = deliveries;
    this.fanout = fanout;
    this.dispatcher = dispatcher;
    this.completion = completion;
    this.clock = clock;
  }

  /** Scans each stage once per cycle; fanout can resume its next page on the following cycle. */
  @Scheduled(
      fixedDelayString = "${tata.notifications.dispatch-delay:5000}",
      initialDelayString = "${tata.notifications.dispatch-initial-delay:5000}")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void dispatch() {
    var cutoff = clock.instant();
    scan(
        cursor ->
            intents.findDueIdsAfter(
                cursor, NotificationIntentState.READY, cutoff, PageRequest.of(0, PAGE_SIZE)),
        fanout::process);
    // Include delivery work committed by this cycle's fanout, retaining a fixed selection cutoff.
    var deliveryCutoff = clock.instant();
    scan(
        cursor ->
            deliveries.findDueIdsAfter(
                cursor,
                NotificationDeliveryState.READY,
                deliveryCutoff,
                PageRequest.of(0, PAGE_SIZE)),
        dispatcher::dispatch);
    scan(
        cursor ->
            intents.findStateIdsAfter(
                cursor, NotificationIntentState.FANOUT_COMPLETE, PageRequest.of(0, PAGE_SIZE)),
        id -> completion.complete(id));
  }

  private static void scan(Function<String, List<String>> selection, Consumer<String> action) {
    String cursor = "";
    while (true) {
      var page = selection.apply(cursor);
      if (page.isEmpty()) return;
      for (String id : page) {
        if (id.compareTo(cursor) <= 0)
          throw new IllegalStateException("notification scan cursor did not advance");
        try {
          action.accept(id);
        } catch (RuntimeException failure) {
          LOGGER.warn("Notification work failed category=NOTIFICATION_WORK_FAILED");
        }
        cursor = id;
      }
      if (page.size() < PAGE_SIZE) return;
    }
  }
}
