package com.vitahealth.tata.treatmentmanagement.interfaces.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.MedicationCatalogQueryService;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MyMedicationsControllerTest {
  private final MedicationCatalogQueryService queries = mock(MedicationCatalogQueryService.class);
  private final MyMedicationsController controller = new MyMedicationsController(queries);

  @Test
  void readsOnlyTheSubjectOfTheOlderAdultSession() {
    var query = new GetMyMedicationCatalogQuery("adult-1");
    when(queries.handle(query)).thenReturn(Result.success(List.of()));
    assertEquals(
        List.of(), controller.list(session(AuthenticatedSession.Role.OLDER_ADULT)).getBody());
    verify(queries).handle(query);
  }

  @Test
  void refusesCaregiverAndSetupSessionsBeforeReadingData() {
    for (var role :
        List.of(AuthenticatedSession.Role.CAREGIVER, AuthenticatedSession.Role.LINK_SETUP)) {
      var error = assertThrows(ResponseStatusException.class, () -> controller.list(session(role)));
      assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }
    verifyNoInteractions(queries);
  }

  @Test
  void refusesRequestsWithoutASession() {
    var error = assertThrows(ResponseStatusException.class, () -> controller.list(null));
    assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    verifyNoInteractions(queries);
  }

  private AuthenticatedSession session(AuthenticatedSession.Role role) {
    return new AuthenticatedSession(
        "adult-1",
        role,
        Instant.now().plusSeconds(60),
        role == AuthenticatedSession.Role.CAREGIVER ? null : "link-1");
  }
}
