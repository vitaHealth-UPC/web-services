package com.tata.treatmentmanagement.infrastructure.events;

import com.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.tata.treatmentmanagement.application.events.TreatmentScheduleEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringTreatmentScheduleEventPublisher implements TreatmentScheduleEventPublisher {
    private final ApplicationEventPublisher publisher;

    public SpringTreatmentScheduleEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(TreatmentScheduleChangedEvent event) {
        publisher.publishEvent(event);
    }
}
