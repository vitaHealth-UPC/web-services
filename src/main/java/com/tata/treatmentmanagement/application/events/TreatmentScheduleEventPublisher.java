package com.tata.treatmentmanagement.application.events;

public interface TreatmentScheduleEventPublisher {
    void publish(TreatmentScheduleChangedEvent event);
}
