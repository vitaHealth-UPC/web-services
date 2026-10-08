package com.tata.intakeexecution.application.internal;

import com.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GetNextIntakeClockTest {
    @Test
    void usesConfiguredTimeToFindTheNextPendingIntake() {
        var now = Instant.parse("2026-10-08T12:00:00Z");
        var repository = mock(IntakeRepository.class);
        when(repository.findNextPendingByOlderAdultId("adult-1", now)).thenReturn(Optional.empty());
        var handler = new GetNextIntakeQueryHandler(repository, Clock.fixed(now, ZoneOffset.UTC));

        assertTrue(handler.handle(" adult-1 ").isEmpty());
        verify(repository).findNextPendingByOlderAdultId("adult-1", now);
    }
}
