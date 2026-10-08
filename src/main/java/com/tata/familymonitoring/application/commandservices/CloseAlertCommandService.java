package com.tata.familymonitoring.application.commandservices;

import com.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;

public interface CloseAlertCommandService {
    AlertSummary handle(CloseAlertCommand command);
}
