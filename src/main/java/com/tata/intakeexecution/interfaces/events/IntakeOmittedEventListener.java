package com.tata.intakeexecution.interfaces.events;

import com.tata.intakeexecution.domain.model.commands.MarkIntakeOmittedCommand;
import com.tata.intakeexecution.application.internal.commandservices.MarkIntakeOmittedCommandHandler;
import com.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("intakeExecutionOmittedEventListener")
public class IntakeOmittedEventListener {

    private final MarkIntakeOmittedCommandHandler handler;

    public IntakeOmittedEventListener(MarkIntakeOmittedCommandHandler handler) {
        this.handler = handler;
    }

    @EventListener
    public void on(IntakeOmitted event) {
        handler.handle(new MarkIntakeOmittedCommand(event.intakeId()));
    }
}
