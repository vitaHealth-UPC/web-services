package com.tata.omissionescalation.application.internal.eventhandlers;

import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.omissionescalation.application.internal.commandservices.OpenOmissionCaseCommandHandler;
import com.tata.omissionescalation.application.internal.commandservices.SendReinforcedReminderCommandHandler;
import com.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeUnconfirmedEventHandler {

  private final OpenOmissionCaseCommandHandler openHandler;
  private final SendReinforcedReminderCommandHandler reminderHandler;
  private final IntakeOmissionPort intakes;

  public IntakeUnconfirmedEventHandler(
      OpenOmissionCaseCommandHandler openHandler,
      SendReinforcedReminderCommandHandler reminderHandler,
      IntakeOmissionPort intakes) {
    this.openHandler = openHandler;
    this.reminderHandler = reminderHandler;
    this.intakes = intakes;
  }

  /** Opens a case only while the source intake is still pending, ignoring delayed events. */
  @Transactional
  public void handle(IntakeUnconfirmed event) {
    if (!intakes.lockPendingIntake(event.intakeId())) return;
    OmissionCase omissionCase =
        openHandler.handle(
            new OpenOmissionCaseCommand(
                event.intakeId(),
                event.olderAdultId(),
                event.medicationName(),
                event.scheduledAt()));
    reminderHandler.handle(new SendReinforcedReminderCommand(omissionCase.getId()));
  }
}
