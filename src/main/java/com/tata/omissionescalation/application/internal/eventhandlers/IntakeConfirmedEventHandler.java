package com.tata.omissionescalation.application.internal.eventhandlers;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.omissionescalation.application.internal.commandservices.ResolveOmissionCaseCommandHandler;
import com.tata.omissionescalation.domain.model.commands.ResolveOmissionCaseCommand;
import org.springframework.stereotype.Service;

@Service
public class IntakeConfirmedEventHandler {

  private final ResolveOmissionCaseCommandHandler resolveHandler;

  public IntakeConfirmedEventHandler(ResolveOmissionCaseCommandHandler resolveHandler) {
    this.resolveHandler = resolveHandler;
  }

  public void handle(IntakeConfirmed event) {
    resolveHandler.handle(new ResolveOmissionCaseCommand(event.intakeId()));
  }
}
