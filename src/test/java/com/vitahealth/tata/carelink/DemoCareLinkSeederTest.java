package com.vitahealth.tata.carelink;

import static org.mockito.Mockito.*;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.DemoCareLinkSeeder;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** Protects recorded demo consent decisions against reinitialization on restart. */
class DemoCareLinkSeederTest {
  @Test
  void existingRelationshipIsPreservedRegardlessOfItsCurrentConsentStatus() {
    var links = mock(CareLinkRepository.class);
    var profiles = mock(OlderAdultProfileRepository.class);
    var events = mock(ApplicationEventPublisher.class);
    var clock = mock(Clock.class);
    when(links.findById("d3a10000-0000-4000-8000-000000000003"))
        .thenReturn(Optional.of(mock(CareLink.class)));
    new DemoCareLinkSeeder(profiles, links, events, clock).run();
    verify(links).findById("d3a10000-0000-4000-8000-000000000003");
    verifyNoMoreInteractions(links);
    verifyNoInteractions(profiles, events, clock);
  }
}
