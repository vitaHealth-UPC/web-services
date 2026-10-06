package com.tata.intakeexecution.application.commands;

import java.time.LocalTime;
import java.util.List;

public record GenerateIntakesCommand(
        String treatmentId,
        String medicationId,
        String olderAdultId,
        String medicationName,
        String dose,
        String frequency,
        List<LocalTime> scheduledTimes,
        String instructions,
        Integer reminderLeadMinutes,
        boolean active
) {
    public GenerateIntakesCommand {
        scheduledTimes = scheduledTimes == null ? List.of() : List.copyOf(scheduledTimes);
    }
}
