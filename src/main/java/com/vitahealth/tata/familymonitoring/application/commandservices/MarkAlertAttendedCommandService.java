package com.vitahealth.tata.familymonitoring.application.commandservices;

import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;

/** Records caregiver attention without closing the pending alert. */
public interface MarkAlertAttendedCommandService {
  /** Returns immutable evidence of the persisted attention state. */
  AlertResult handle(MarkAlertAttendedCommand command);
}
