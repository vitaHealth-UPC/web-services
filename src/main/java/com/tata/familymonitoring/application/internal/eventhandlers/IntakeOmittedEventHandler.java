package com.tata.familymonitoring.application.internal.eventhandlers;

import com.tata.familymonitoring.application.internal.commandservices.RegisterAlertFromOmissionCommandHandler;
import com.tata.familymonitoring.domain.model.commands.RegisterAlertFromOmissionCommand;
import com.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.stereotype.Service;

@Service
public class IntakeOmittedEventHandler {

  private final RegisterAlertFromOmissionCommandHandler registerAlertHandler;

  public IntakeOmittedEventHandler(RegisterAlertFromOmissionCommandHandler registerAlertHandler) {
    this.registerAlertHandler = registerAlertHandler;
  }

  public void handle(IntakeOmitted event) {
    registerAlertHandler.handle(new RegisterAlertFromOmissionCommand(
        event.olderAdultId(),
        event.intakeId(),
        event.medicationName(),
        event.scheduledAt(),
        event.reason()));
  }
}
