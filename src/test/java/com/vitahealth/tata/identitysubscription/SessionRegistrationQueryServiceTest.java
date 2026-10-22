package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionRegistrationLookup;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.internal.queryservices.SessionRegistrationQueryServiceImpl;
import com.vitahealth.tata.identitysubscription.application.result.RegisteredSessionResult;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Verifies the credential check cannot be bypassed through device registration reference lookup.
 */
class SessionRegistrationQueryServiceTest {
  @Test
  void invalidCredentialDoesNotResolvePersistedReference() {
    var tokens = mock(SessionTokenService.class);
    var lookup = mock(SessionRegistrationLookup.class);
    when(tokens.authenticate("invalid")).thenReturn(Optional.empty());
    var service = new SessionRegistrationQueryServiceImpl(tokens, lookup);
    assertThat(service.authenticateRegistration("invalid")).isEmpty();
    verifyNoInteractions(lookup);
  }

  @Test
  void identityChangeBetweenCredentialAndRegistrationLookupIsRejected() {
    var tokens = mock(SessionTokenService.class);
    var lookup = mock(SessionRegistrationLookup.class);
    var identity = new AuthenticatedSession("owner", Role.CAREGIVER, Instant.MAX, null);
    when(tokens.authenticate("credential")).thenReturn(Optional.of(identity));
    var other = new AuthenticatedSession("other", Role.CAREGIVER, Instant.MAX, null);
    when(lookup.findRegistration("credential"))
        .thenReturn(Optional.of(new RegisteredSessionResult("session", other)));
    var service = new SessionRegistrationQueryServiceImpl(tokens, lookup);
    assertThat(service.authenticateRegistration("credential")).isEmpty();
    when(lookup.findRegistration("credential"))
        .thenReturn(Optional.of(new RegisteredSessionResult("session", identity)));
    assertThat(service.authenticateRegistration("credential"))
        .hasValueSatisfying(result -> assertThat(result.sessionId()).isEqualTo("session"));
  }
}
