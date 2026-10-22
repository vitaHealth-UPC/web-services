package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.infrastructure.acl.ContactChannelAdapter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Protects contact mapping from foreign profile evidence while preserving valid absence. */
class ContactChannelAdapterTest {
  private final CareLinkContextFacade source = mock(CareLinkContextFacade.class);
  private final ContactChannelAdapter adapter = new ContactChannelAdapter(source);

  private OlderAdultProfileResult profile(String owner, String phone) {
    return new OlderAdultProfileResult(
        owner,
        "caregiver",
        "Adult",
        LocalDate.of(1950, 1, 1),
        null,
        null,
        phone,
        Instant.parse("2026-10-19T12:00:00Z"));
  }

  @Test
  void foreignProfileIsRejectedEvenWhenItsContactIsAbsent() {
    when(source.findOlderAdult("adult")).thenReturn(Optional.of(profile("foreign", null)));
    var failure =
        assertThrows(
            InvalidMonitoringSourceException.class, () -> adapter.findContactChannel("adult"));
    assertInstanceOf(IllegalStateException.class, failure.getCause());
  }

  @Test
  void matchingMissingProfileAndPhoneAreValidAbsence() {
    when(source.findOlderAdult("adult")).thenReturn(Optional.empty());
    assertTrue(adapter.findContactChannel("adult").isEmpty());
    when(source.findOlderAdult("adult")).thenReturn(Optional.of(profile("adult", null)));
    assertTrue(adapter.findContactChannel("adult").isEmpty());
    when(source.findOlderAdult("adult")).thenReturn(Optional.of(profile("adult", " ")));
    assertTrue(adapter.findContactChannel("adult").isEmpty());
  }

  @Test
  void registeredPhoneFormattingIsPreserved() {
    when(source.findOlderAdult("adult"))
        .thenReturn(Optional.of(profile("adult", " +51 999 123 456 ")));
    assertEquals(" +51 999 123 456 ", adapter.findContactChannel("adult").orElseThrow().value());
  }

  @Test
  void technicalSourceCauseIsRetainedWithoutCopyingItsDetailsToThePublicMessage() {
    var cause = new IllegalArgumentException("internal-source-marker");
    when(source.findOlderAdult("adult")).thenThrow(cause);
    var failure =
        assertThrows(
            InvalidMonitoringSourceException.class, () -> adapter.findContactChannel("adult"));
    assertSame(cause, failure.getCause());
    assertEquals("Monitoring source evidence is inconsistent", failure.getMessage());
  }
}
