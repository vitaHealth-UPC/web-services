package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.carelink.application.internal.queryservices.FindOlderAdultProfileByIdQueryHandler;
import com.vitahealth.tata.carelink.domain.model.queries.FindOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

/** Distinguishes absent profiles from unavailable profile persistence. */
class OptionalProfileLookupTest {
  @Test
  void missingProfileReturnsEmpty() {
    var profiles = mock(OlderAdultProfileRepository.class);
    when(profiles.findById("missing")).thenReturn(Optional.empty());
    assertTrue(
        new FindOlderAdultProfileByIdQueryHandler(profiles)
            .handle(new FindOlderAdultProfileByIdQuery("missing"))
            .isEmpty());
  }

  @Test
  void technicalFailureCannotBecomeMissingProfile() {
    var profiles = mock(OlderAdultProfileRepository.class);
    var failure = new DataAccessResourceFailureException("database unavailable");
    when(profiles.findById("adult")).thenThrow(failure);
    var handler = new FindOlderAdultProfileByIdQueryHandler(profiles);
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> handler.handle(new FindOlderAdultProfileByIdQuery("adult"))));
  }
}
