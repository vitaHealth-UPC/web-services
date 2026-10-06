package com.tata.intakeexecution.interfaces.events;

import com.tata.intakeexecution.application.commands.GenerateIntakesCommand;
import com.tata.intakeexecution.application.internal.commandservices.GenerateIntakesCommandHandler;
import com.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TreatmentScheduleChangedEventListener {
    private final GenerateIntakesCommandHandler handler;

    public TreatmentScheduleChangedEventListener(GenerateIntakesCommandHandler handler) {
        this.handler = handler;
    }

    @EventListener
    public void on(TreatmentScheduleChangedEvent event) {
        handler.handle(new GenerateIntakesCommand(
                event.treatmentId(),
                event.medicationId(),
                event.olderAdultId(),
                event.medicationName(),
                event.dose(),
                event.frequency(),
                event.scheduledTimes(),
                event.instructions(),
                event.reminderLeadMinutes(),
                event.active()
        ));
    }
}
