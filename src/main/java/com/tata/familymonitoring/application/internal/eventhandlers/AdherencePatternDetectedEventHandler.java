package com.tata.familymonitoring.application.internal.eventhandlers;

import com.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.tata.familymonitoring.application.internal.commandservices.RegisterAdherenceInsightCommandHandler;
import com.tata.familymonitoring.domain.model.commands.RegisterAdherenceInsightCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class AdherencePatternDetectedEventHandler {
  private final RegisterAdherenceInsightCommandHandler registerInsight;

  public AdherencePatternDetectedEventHandler(RegisterAdherenceInsightCommandHandler registerInsight) {
    this.registerInsight = registerInsight;
  }

  @EventListener
  public void handle(AdherencePatternDetected event) {
    registerInsight.handle(new RegisterAdherenceInsightCommand(
        event.olderAdultId(), event.medicationId(), event.omissionDays(),
        event.firstDay(), event.lastDay(), event.detectedAt()));
  }
}
