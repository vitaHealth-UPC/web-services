package com.vitahealth.tata.identitysubscription.infrastructure.tokens;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt.JwtSessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt.JwtTokenCodec;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.opaque.OpaqueSessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence.PersistedSessionRegistry;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/** Both credential encodings must preserve the relationship selected by consent. */
class ExactCareLinkSessionIssuanceTest {
  private final CareLinkContextFacade links = mock(CareLinkContextFacade.class);
  private final PersistedSessionRegistry registry = mock(PersistedSessionRegistry.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

  private SessionTokenService service(boolean jwt) {
    var opaque = new OpaqueSessionTokenService(links, clock, registry);
    if (!jwt) return opaque;
    var codec = mock(JwtTokenCodec.class);
    when(codec.encode(anyString(), any())).thenReturn("signed-token");
    return new JwtSessionTokenService(codec, opaque, links, clock, registry);
  }

  private CareLinkResult link(String adult, CareLinkStatus status, boolean consent) {
    return new CareLinkResult(
        "selected-link",
        "caregiver",
        adult,
        status,
        "TATA-4821",
        clock.instant().plusSeconds(600),
        clock.instant().minusSeconds(60),
        consent,
        consent ? clock.instant() : null,
        consent ? clock.instant() : null);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preservesSelectedRelationshipWithoutSearchingForAnother(boolean jwt) {
    when(links.getById("selected-link")).thenReturn(link("adult", CareLinkStatus.CONFIRMED, true));
    service(jwt).issueOlderAdult("adult", "selected-link");
    var identity = ArgumentCaptor.forClass(AuthenticatedSession.class);
    verify(registry).register(anyString(), anyString(), identity.capture());
    assertEquals("selected-link", identity.getValue().careLinkId());
    assertEquals("adult", identity.getValue().subjectId());
    verify(links, never()).getConfirmedByCaregiver(anyString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rejectsForeignAdultBeforePersistingCredential(boolean jwt) {
    when(links.getById("selected-link"))
        .thenReturn(link("other-adult", CareLinkStatus.CONFIRMED, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> service(jwt).issueOlderAdult("adult", "selected-link"));
    verifyNoInteractions(registry);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rejectsRevokedRelationshipEvenWithOldConfirmationEvidence(boolean jwt) {
    when(links.getById("selected-link")).thenReturn(link("adult", CareLinkStatus.REVOKED, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> service(jwt).issueOlderAdult("adult", "selected-link"));
    verifyNoInteractions(registry);
  }
}
