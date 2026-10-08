package com.tata.omissionescalation.application.internal.eventhandlers;

import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.omissionescalation.application.internal.commandservices.OpenOmissionCaseCommandHandler;
import com.tata.omissionescalation.application.internal.commandservices.SendReinforcedReminderCommandHandler;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import org.springframework.stereotype.Service;

@Service
public class IntakeUnconfirmedEventHandler {

  private final OpenOmissionCaseCommandHandler openHandler;
  private final SendReinforcedReminderCommandHandler reminderHandler;

  public IntakeUnconfirmedEventHandler(
      OpenOmissionCaseCommandHandler openHandler,
      SendReinforcedReminderCommandHandler reminderHandler) {
    this.openHandler = openHandler;
    this.reminderHandler = reminderHandler;
  }

  /** Opens the case and requests the reinforced reminder. */
  public void handle(IntakeUnconfirmed event) {
    OmissionCase omissionCase = openHandler.handle(new OpenOmissionCaseCommand(
        event.intakeId(), event.olderAdultId(), event.medicationName(), event.scheduledAt()));
    reminderHandler.handle(new SendReinforcedReminderCommand(omissionCase.getId()));
  }
}
