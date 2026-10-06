package com.tata.treatmentmanagement.application.events;

import java.time.LocalTime;
import java.util.List;

public record TreatmentScheduleChangedEvent(
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
    public TreatmentScheduleChangedEvent {
        scheduledTimes = scheduledTimes == null ? List.of() : List.copyOf(scheduledTimes);
    }
}
