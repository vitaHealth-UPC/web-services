package com.vitahealth.tata.familymonitoring.application.commandservices;

import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CloseAlertCommand;

/** Closes an owned alert while preserving the original omission history. */
public interface CloseAlertCommandService {
  /** Returns immutable evidence of the persisted explicit closure. */
  AlertResult handle(CloseAlertCommand command);
}
