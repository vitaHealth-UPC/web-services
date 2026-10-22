package com.vitahealth.tata.carelink.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.internal.outboundservices.AccountStatusPort;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeAllocationPort;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Verifies issuance uses the injected time source and rejects foreign profile ownership. */
class GenerateLinkingCodeCommandHandlerTest {
  private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

  @Test
  void oneInjectedInstantControlsCreationAndCodeLifetime() {
    var profiles = mock(OlderAdultProfileRepository.class);
    var links = mock(CareLinkRepository.class);
    var accounts = mock(AccountStatusPort.class);
    var codes = mock(LinkingCodeGenerator.class);
    var profile =
        OlderAdultProfile.register(
            "caregiver", new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)), null, NOW);
    when(accounts.isEnabled("caregiver")).thenReturn(true);
    when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
    when(codes.generate()).thenReturn("TATA-4821");
    when(links.save(any(CareLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
    var handler =
        new GenerateLinkingCodeCommandHandler(
            profiles,
            links,
            new EnabledCaregiverAccountGuard(accounts),
            codes,
            Clock.fixed(NOW, ZoneOffset.UTC),
            mock(LinkingCodeAllocationPort.class));
    handler.handle(new GenerateLinkingCodeCommand("caregiver", profile.id()));
    var captured = ArgumentCaptor.forClass(CareLink.class);
    verify(links).save(captured.capture());
    assertThat(captured.getValue().createdAt()).isEqualTo(NOW);
    assertThat(captured.getValue().updatedAt()).isEqualTo(NOW);
    assertThat(captured.getValue().linkingCode().expiresAt())
        .isEqualTo(NOW.plus(Duration.ofMinutes(15)));
  }

  @Test
  void foreignProfileCannotGenerateOrPersistACode() {
    var profiles = mock(OlderAdultProfileRepository.class);
    var links = mock(CareLinkRepository.class);
    var accounts = mock(AccountStatusPort.class);
    var codes = mock(LinkingCodeGenerator.class);
    var profile =
        OlderAdultProfile.register(
            "owner", new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)), null, NOW);
    when(accounts.isEnabled("foreign")).thenReturn(true);
    when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
    var handler =
        new GenerateLinkingCodeCommandHandler(
            profiles,
            links,
            new EnabledCaregiverAccountGuard(accounts),
            codes,
            Clock.fixed(NOW, ZoneOffset.UTC),
            mock(LinkingCodeAllocationPort.class));
    assertThatThrownBy(
            () -> handler.handle(new GenerateLinkingCodeCommand("foreign", profile.id())))
        .isInstanceOf(CareLinkApplicationException.class)
        .satisfies(
            exception ->
                assertThat(((CareLinkApplicationException) exception).code())
                    .isEqualTo(CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND));
    verifyNoInteractions(codes, links);
  }
}
