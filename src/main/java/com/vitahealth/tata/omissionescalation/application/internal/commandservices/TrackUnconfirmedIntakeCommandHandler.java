package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically tracks an intake that remains pending and queues its first durable reminder. */
@Service
public class TrackUnconfirmedIntakeCommandHandler {

  private final OpenOmissionCaseCommandHandler openHandler;
  private final SendReinforcedReminderCommandHandler reminderHandler;
  private final IntakeOmissionPort intakes;

  /**
   * Injects source reservation and the two local command responsibilities sharing its transaction.
   */
  public TrackUnconfirmedIntakeCommandHandler(
      OpenOmissionCaseCommandHandler openHandler,
      SendReinforcedReminderCommandHandler reminderHandler,
      IntakeOmissionPort intakes) {
    this.openHandler = openHandler;
    this.reminderHandler = reminderHandler;
    this.intakes = intakes;
  }

  /** Opens a case only while the source intake is still pending, ignoring delayed events. */
  @Transactional
  public void handle(OpenOmissionCaseCommand command) {
    if (!intakes.lockPendingIntake(command.intakeId())) return;
    OmissionCase omissionCase = openHandler.handle(command);
    reminderHandler.handle(new SendReinforcedReminderCommand(omissionCase.getId()));
  }
}
