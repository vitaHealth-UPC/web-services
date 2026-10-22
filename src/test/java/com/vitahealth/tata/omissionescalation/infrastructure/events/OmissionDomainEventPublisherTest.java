package com.vitahealth.tata.omissionescalation.infrastructure.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.omissionescalation.domain.model.events.CaregiverAlertGenerated;
import com.vitahealth.tata.omissionescalation.domain.model.events.EscalationExecuted;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import com.vitahealth.tata.omissionescalation.domain.model.events.OmissionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;

/** Exercises concrete Spring event routing and synchronous consumer failure propagation. */
class OmissionDomainEventPublisherTest {

  @Test
  void dispatchesEveryConcreteBusinessFactBeforeReturning() {
    try (var context =
        new AnnotationConfigApplicationContext(
            OmissionDomainEventPublisher.class, Observer.class)) {
      var publisher = context.getBean(OmissionDomainEventPublisher.class);
      var observer = context.getBean(Observer.class);
      Instant occurredAt = Instant.parse("2026-10-18T10:00:00Z");
      List<OmissionEvent> events =
          List.of(
              new IntakeOmitted("intake", "adult", "medication", occurredAt, "omitted", occurredAt),
              new CaregiverAlertGenerated(1L, "intake", "adult", occurredAt),
              new EscalationExecuted(1L, "intake", "adult", 1, occurredAt));

      for (OmissionEvent event : events) {
        publisher.publish(event);
        assertSame(event, observer.received.getLast());
      }
      assertEquals(events, observer.received);
    }
  }

  @Test
  void propagatesTheConsumerFailureWithoutReplacingIt() {
    try (var context =
        new AnnotationConfigApplicationContext(
            OmissionDomainEventPublisher.class, Observer.class)) {
      var publisher = context.getBean(OmissionDomainEventPublisher.class);
      var observer = context.getBean(Observer.class);
      observer.failure = new IllegalStateException("consumer cannot persist the projection");

      var failure =
          assertThrows(
              IllegalStateException.class,
              () ->
                  publisher.publish(
                      new CaregiverAlertGenerated(1L, "intake", "adult", Instant.now())));

      assertSame(observer.failure, failure);
    }
  }

  /** Uses concrete listener parameter types, as neighboring bounded contexts do. */
  static class Observer {
    final List<OmissionEvent> received = new ArrayList<>();
    RuntimeException failure;

    @EventListener
    public void onOmission(IntakeOmitted event) {
      accept(event);
    }

    @EventListener
    public void onAlert(CaregiverAlertGenerated event) {
      accept(event);
    }

    @EventListener
    public void onEscalation(EscalationExecuted event) {
      accept(event);
    }

    private void accept(OmissionEvent event) {
      if (failure != null) throw failure;
      received.add(event);
    }
  }
}
