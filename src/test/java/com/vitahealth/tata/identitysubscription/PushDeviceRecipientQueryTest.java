package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.PushDeviceCandidateLookup;
import com.vitahealth.tata.identitysubscription.application.internal.queryservices.PushDeviceQueryServiceImpl;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Checks revocation between storage selection and live authorization without caching a principal.
 */
class PushDeviceRecipientQueryTest {
  @Test
  void revokedCandidateStillAdvancesTheScanCursor() {
    var candidates = mock(PushDeviceCandidateLookup.class);
    var sessions = mock(SessionRegistrationQueryService.class);
    var now = Instant.parse("2026-10-17T12:00:00Z");
    var device =
        PushDeviceRegistration.register(
            UUID.randomUUID().toString(), "revoked", "private-token", now);
    when(candidates.findCandidates(any(), any(), any(), anyInt(), any()))
        .thenReturn(List.of(device));
    when(sessions.findActiveRegistration("revoked")).thenReturn(Optional.empty());
    var queries =
        new PushDeviceQueryServiceImpl(
            candidates,
            sessions,
            Clock.fixed(now, ZoneOffset.UTC),
            mock(PushDeviceRegistrationRepository.class));
    var page = queries.findActiveDevices("owner", Role.CAREGIVER, "", 1);
    assertThat(page.devices()).isEmpty();
    assertThat(page.nextCursor()).isEqualTo(device.installationId());
  }

  @Test
  void invalidScopeAndUnboundedRequestsNeverReadDeviceStorage() {
    var candidates = mock(PushDeviceCandidateLookup.class);
    var sessions = mock(SessionRegistrationQueryService.class);
    var queries =
        new PushDeviceQueryServiceImpl(
            candidates, sessions, Clock.systemUTC(), mock(PushDeviceRegistrationRepository.class));
    assertThatThrownBy(() -> queries.findActiveDevices("owner", Role.LINK_SETUP, "", 100))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queries.findActiveDevices("owner", Role.CAREGIVER, "", 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queries.findActiveDevices("owner", Role.CAREGIVER, "", 0))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(candidates, sessions);
  }
}
