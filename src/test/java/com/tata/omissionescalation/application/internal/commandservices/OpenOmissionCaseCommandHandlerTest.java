package com.tata.omissionescalation.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tata.omissionescalation.domain.factories.OmissionCaseFactory;
import com.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenOmissionCaseCommandHandlerTest {
  @Mock IOmissionCaseRepository repository;
  private static final Instant SCHEDULED = Instant.parse("2026-10-01T13:00:00Z");
  private static final Duration GRACE = Duration.ofMinutes(30);
  private final OpenOmissionCaseCommand command = new OpenOmissionCaseCommand("dose", "adult", "Medication", SCHEDULED);

  @Test void delayedProcessingDoesNotExtendTheConfirmationPeriod() {
    when(repository.findByIntakeId("dose")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var result = new OpenOmissionCaseCommandHandler(repository, GRACE).handle(command);
    assertThat(result.getGracePeriod().startsAt()).isEqualTo(SCHEDULED);
    assertThat(result.getGracePeriod().endsAt()).isEqualTo(SCHEDULED.plus(GRACE));
    assertThat(result.isGraceExpired(SCHEDULED.plus(GRACE))).isTrue();
  }

  @Test void duplicateEventKeepsTheOriginalGracePeriod() {
    var original = OmissionCaseFactory.createPending("dose", "adult", "Medication", SCHEDULED,
        GracePeriod.startingAt(SCHEDULED, GRACE));
    when(repository.findByIntakeId("dose")).thenReturn(Optional.of(original));
    var laterEvent = new OpenOmissionCaseCommand("dose", "adult", "Medication", SCHEDULED.plusSeconds(600));
    assertThat(new OpenOmissionCaseCommandHandler(repository, GRACE).handle(laterEvent)).isSameAs(original);
    assertThat(original.getGracePeriod().endsAt()).isEqualTo(SCHEDULED.plus(GRACE));
    verify(repository, never()).save(any());
  }
}

