package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.internal.commandservices.AcceptCareLinkCommandHandler;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies credential input bounds independently of HTTP validation. */
class LinkingEvidenceInputTest {
  @Test
  void overlongEvidenceCannotReachRepositoryLookup() {
    var links = mock(CareLinkRepository.class);
    var accounts = mock(EnabledCaregiverAccountGuard.class);
    var handler = new AcceptCareLinkCommandHandler(links, accounts, Clock.systemUTC());
    var failure =
        assertThrows(
            CareLinkApplicationException.class,
            () -> handler.handle(new AcceptCareLinkCommand("caregiver", "x".repeat(33))));
    assertThat(failure.code()).isEqualTo(CareLinkApplicationException.Code.INVALID_LINKING_CODE);
    verifyNoInteractions(links);
  }

  @Test
  void issuedAndRestoredEvidenceRespectTheSameStorageBound() {
    var now = Instant.parse("2026-10-14T12:00:00Z");
    assertThat(LinkingCode.issue("x".repeat(32), now, Duration.ofMinutes(1)).value()).hasSize(32);
    assertThrows(
        IllegalArgumentException.class,
        () -> LinkingCode.issue("x".repeat(33), now, Duration.ofMinutes(1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> LinkingCode.rehydrate("x".repeat(33), now.plusSeconds(60), null));
  }
}
