package com.vitahealth.tata.familymonitoring.interfaces.events;

import com.vitahealth.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.RegisterAdherenceInsightCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.commands.RegisterAdherenceInsightCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Maps analytics pattern evidence to the local adherence-insight command. */
@Component
public class AdherencePatternDetectedEventConsumer {
  private final RegisterAdherenceInsightCommandHandler registerInsight;

  /** Injects the local insight registration boundary. */
  public AdherencePatternDetectedEventConsumer(
      RegisterAdherenceInsightCommandHandler registerInsight) {
    this.registerInsight = registerInsight;
  }

  /** Preserves the detected period and occurrence time during synchronous dispatch. */
  @EventListener
  public void consume(AdherencePatternDetected event) {
    registerInsight.handle(
        new RegisterAdherenceInsightCommand(
            event.olderAdultId(),
            event.medicationId(),
            event.omissionDays(),
            event.firstDay(),
            event.lastDay(),
            event.detectedAt()));
  }
}
