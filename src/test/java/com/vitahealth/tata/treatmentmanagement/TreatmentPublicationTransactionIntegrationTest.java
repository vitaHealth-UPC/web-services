package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

/** Ensures a schedule cannot be published outside its owning database transaction. */
@SpringBootTest
@ActiveProfiles("test")
class TreatmentPublicationTransactionIntegrationTest {
  @Autowired TreatmentScheduleEventPublisher events;

  @Test
  void rejectsUncoordinatedPublicationBeforeInvokingConsumers() {
    var event =
        new TreatmentScheduleChangedEvent(
            "treatment",
            "medication",
            "adult",
            "Losartán",
            "1 comprimido",
            "DAILY",
            List.of(LocalTime.NOON),
            "",
            10,
            true);
    assertThrows(IllegalTransactionStateException.class, () -> events.publish(event));
  }
}
