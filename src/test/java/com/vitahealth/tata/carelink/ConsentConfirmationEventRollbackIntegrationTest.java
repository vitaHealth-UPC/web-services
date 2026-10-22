package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

/** Verifies an event-consumer failure rolls back both intermediate approval and confirmation. */
@SpringBootTest
@ActiveProfiles("test")
class ConsentConfirmationEventRollbackIntegrationTest {
  static class FailingConsumer {
    private final CareLinkRepository links;
    final AtomicReference<String> target = new AtomicReference<>();
    boolean observedConfirmation;

    FailingConsumer(CareLinkRepository links) {
      this.links = links;
    }

    @EventListener
    @Order(100)
    void reject(CareLinkConfirmed event) {
      if (event.careLinkId().equals(target.get())) {
        observedConfirmation = links.findById(event.careLinkId()).orElseThrow().isActive();
        throw new IllegalStateException("simulated confirmation consumer failure");
      }
    }
  }

  @TestConfiguration
  static class Failure {
    @Bean
    FailingConsumer failingConsumer(CareLinkRepository links) {
      return new FailingConsumer(links);
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkCommandService commands;
  @Autowired private CareLinkRepository links;
  @Autowired private FailingConsumer consumer;

  @Test
  void failureAfterConfirmedStateIsVisibleRestoresUnrecordedConsent() {
    var fixture = CareLinkHttpFixture.awaiting(context);
    consumer.target.set(fixture.linkId());
    try {
      assertThatThrownBy(
              () ->
                  commands
                      .registerConsent(new RegisterConsentCommand(fixture.linkId(), true))
                      .toOptional()
                      .orElseThrow())
          .isInstanceOf(IllegalStateException.class);
      assertThat(consumer.observedConfirmation).isTrue();
      var restored = links.findById(fixture.linkId()).orElseThrow();
      assertThat(restored.status()).isEqualTo(CareLinkStatus.AWAITING_CONSENT);
      assertThat(restored.consent().recordedAt()).isNull();
      assertThat(restored.confirmedAt()).isNull();
    } finally {
      consumer.target.set(null);
    }
  }
}
