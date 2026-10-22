package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Rejects malformed work pages before repeated references can cause an unbounded evaluation. */
class EvaluateGracePeriodCommandHandlerTest {

  @Test
  void repeatedReferenceStopsTheScanBeforeExecutingTheSameCaseTwice() {
    var repository = mock(IOmissionCaseRepository.class);
    var register = mock(RegisterOmissionCommandHandler.class);
    var alerts = mock(GenerateCaregiverAlertCommandHandler.class);
    var escalate = mock(EscalateOmissionCommandHandler.class);
    var evaluator = new EvaluateGracePeriodCommandHandler(repository, register, alerts, escalate);
    var now = Instant.parse("2026-10-18T12:00:00Z");
    when(repository.findActiveCaseIdsWithoutAlertAfter(0, 100)).thenReturn(List.of(1L, 1L));

    assertThrows(
        IllegalStateException.class, () -> evaluator.handle(new EvaluateGracePeriodCommand(now)));

    verify(alerts).handle(new GenerateCaregiverAlertCommand(1L, now));
    verifyNoMoreInteractions(alerts);
    verifyNoInteractions(register, escalate);
  }
}
