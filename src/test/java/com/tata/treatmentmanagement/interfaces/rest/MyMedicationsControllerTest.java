package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.identitysubscription.application.models.AuthenticatedSession;
import com.tata.treatmentmanagement.application.queryservices.MedicationCatalogQueryService;
import com.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyMedicationsControllerTest {
    private final MedicationCatalogQueryService queries = mock(MedicationCatalogQueryService.class);
    private final MyMedicationsController controller = new MyMedicationsController(queries);

    @Test
    void readsOnlyTheSubjectOfTheOlderAdultSession() {
        var query = new GetMyMedicationCatalogQuery("adult-1");
        when(queries.handle(query)).thenReturn(List.of());
        assertEquals(List.of(), controller.list(session(AuthenticatedSession.Role.OLDER_ADULT)));
        verify(queries).handle(query);
    }

    @Test
    void refusesCaregiverAndSetupSessionsBeforeReadingData() {
        for (var role : List.of(AuthenticatedSession.Role.CAREGIVER, AuthenticatedSession.Role.LINK_SETUP)) {
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
        return new AuthenticatedSession("adult-1", role, Instant.now().plusSeconds(60), null);
    }
}
