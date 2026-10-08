package com.tata.familymonitoring.application.commandservices;

import com.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;

public interface MarkAlertAttendedCommandService {
    AlertSummary handle(MarkAlertAttendedCommand command);
}
