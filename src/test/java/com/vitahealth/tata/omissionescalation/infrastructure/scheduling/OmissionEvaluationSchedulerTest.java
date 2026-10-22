package com.vitahealth.tata.omissionescalation.infrastructure.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vitahealth.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifies that scheduled decisions use the injected clinical evaluation instant. */
class OmissionEvaluationSchedulerTest {
  @Test
  void evaluatesAtTheConfiguredClockInstant() {
    var handler = mock(EvaluateGracePeriodCommandHandler.class);
    var now = Instant.parse("2026-10-11T12:00:00Z");
    var scheduler = new OmissionEvaluationScheduler(handler, Clock.fixed(now, ZoneOffset.UTC));
    scheduler.evaluatePendingCases();
    verify(handler).handle(new EvaluateGracePeriodCommand(now));
  }
}
