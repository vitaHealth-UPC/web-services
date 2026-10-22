package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationCompletionProcessor;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryScheduler;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeviceDispatcher;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationFanoutPageProcessor;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Verifies bounded cursor progress isolates a poison record without exposing its exception text.
 */
@ExtendWith(OutputCaptureExtension.class)
class NotificationDeliverySchedulerTest {
  @Test
  void failedFirstRecordCannotStarveTheSecondPage(CapturedOutput output) {
    var intents = mock(NotificationIntentJpaRepository.class);
    var deliveries = mock(NotificationDeviceDeliveryJpaRepository.class);
    var fanout = mock(NotificationFanoutPageProcessor.class);
    var dispatcher = mock(NotificationDeviceDispatcher.class);
    var completion = mock(NotificationCompletionProcessor.class);
    var firstPage =
        IntStream.rangeClosed(1, 100).mapToObj(index -> String.format("%03d", index)).toList();
    when(intents.findDueIdsAfter(eq(""), eq(NotificationIntentState.READY), any(), any()))
        .thenReturn(firstPage);
    when(intents.findDueIdsAfter(eq("100"), eq(NotificationIntentState.READY), any(), any()))
        .thenReturn(List.of("101"));
    doThrow(new IllegalStateException("private-medication private-token"))
        .when(fanout)
        .process("001");
    new NotificationDeliveryScheduler(
            intents, deliveries, fanout, dispatcher, completion, Clock.systemUTC())
        .dispatch();
    verify(fanout).process("101");
    assertThat(output.getAll())
        .contains("category=NOTIFICATION_WORK_FAILED")
        .doesNotContain("private-medication", "private-token");
  }
}
