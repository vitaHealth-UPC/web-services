package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.commandservices.PushDeviceMutationHandler;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.identitysubscription.application.result.RegisteredSessionResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Reproduces a request whose row reservation observes a rotation newer than its initial time. */
class PushDeviceMutationHandlerTest {
  @Test
  void delayedRotationUsesWriteTimeAfterTheReservedSnapshot() {
    var initial = Instant.parse("2026-10-17T15:00:00Z");
    var devices = mock(PushDeviceRegistrationRepository.class);
    var sessions = mock(SessionRegistrationQueryService.class);
    var clock = mock(Clock.class);
    var id = UUID.randomUUID().toString();
    var previous =
        PushDeviceRegistration.register(id, "session", "old", initial)
            .rotate("intermediate", initial.plusSeconds(1));
    when(clock.instant()).thenReturn(initial, initial.plusSeconds(2));
    when(sessions.authenticateRegistration("credential"))
        .thenReturn(
            Optional.of(
                new RegisteredSessionResult(
                    "session",
                    new AuthenticatedSession("owner", Role.CAREGIVER, Instant.MAX, null))));
    when(devices.findByInstallationIdForUpdate(id)).thenReturn(Optional.of(previous));
    when(devices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var handler = new PushDeviceMutationHandler(sessions, devices, clock);
    var result = handler.register(new RegisterPushDeviceCommand(id, "latest"), "credential");
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.toOptional().orElseThrow().updatedAt()).isEqualTo(initial.plusSeconds(2));
  }
}
