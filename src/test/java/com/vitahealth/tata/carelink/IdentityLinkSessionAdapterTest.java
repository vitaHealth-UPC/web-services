package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.carelink.infrastructure.acl.IdentityLinkSessionAdapter;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Checks the consent lifecycle before any credential crosses the Identity boundary. */
class IdentityLinkSessionAdapterTest {
  private final IdentityContextFacade identity = mock(IdentityContextFacade.class);
  private final IdentityLinkSessionAdapter adapter = new IdentityLinkSessionAdapter(identity);
  private final Instant now = Instant.parse("2026-10-13T12:00:00Z");

  private CareLinkResult link(CareLinkStatus status, boolean consent) {
    return new CareLinkResult(
        "selected-link",
        "caregiver",
        "adult",
        status,
        "TATA-4821",
        now.plusSeconds(600),
        now,
        consent,
        consent ? now : null,
        consent ? now : null);
  }

  @Test
  void confirmedAccessUsesExactLink() {
    when(identity.issueOlderAdult("adult", "selected-link"))
        .thenReturn(new SessionResult("adult", "credential", now.plusSeconds(600)));
    assertEquals(
        "credential", adapter.establish(link(CareLinkStatus.CONFIRMED, true)).accessToken());
    verify(identity).issueOlderAdult("adult", "selected-link");
    verifyNoMoreInteractions(identity);
  }

  @Test
  void acceptedRequestUsesRestrictedOnboarding() {
    when(identity.issueLinkSetup("adult", "selected-link"))
        .thenReturn(new SessionResult("adult", "setup", now.plusSeconds(600)));
    adapter.establish(link(CareLinkStatus.AWAITING_CONSENT, false));
    verify(identity).issueLinkSetup("adult", "selected-link");
    verifyNoMoreInteractions(identity);
  }

  @ParameterizedTest
  @EnumSource(
      value = CareLinkStatus.class,
      names = {"PENDING", "REVOKED", "CONFIRMED"})
  void invalidStateCannotFallBackToOnboarding(CareLinkStatus status) {
    assertThrows(IllegalArgumentException.class, () -> adapter.establish(link(status, false)));
    verifyNoInteractions(identity);
  }
}
