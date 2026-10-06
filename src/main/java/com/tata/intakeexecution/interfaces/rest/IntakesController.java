package com.tata.intakeexecution.interfaces.rest;

import com.tata.intakeexecution.application.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeCommandHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetIntakeDetailQueryHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.interfaces.rest.resources.ConfirmIntakeResource;
import com.tata.intakeexecution.interfaces.rest.resources.IntakeResource;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IntakesController {
    private final GetNextIntakeQueryHandler getNextIntake;
    private final GetIntakeDetailQueryHandler getIntakeDetail;
    private final ConfirmIntakeCommandHandler confirmIntake;

    public IntakesController(
            GetNextIntakeQueryHandler getNextIntake,
            GetIntakeDetailQueryHandler getIntakeDetail,
            ConfirmIntakeCommandHandler confirmIntake
    ) {
        this.getNextIntake = getNextIntake;
        this.getIntakeDetail = getIntakeDetail;
        this.confirmIntake = confirmIntake;
    }

    @GetMapping("/older-adults/{olderAdultId}/intakes/next")
    public ResponseEntity<IntakeResource> next(@PathVariable String olderAdultId) {
        return getNextIntake.handle(olderAdultId)
                .map(this::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/intakes/{intakeId}")
    public ResponseEntity<IntakeResource> detail(@PathVariable String intakeId) {
        return getIntakeDetail.handle(intakeId)
                .map(this::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/intakes/{intakeId}/confirmation")
    public IntakeResource confirm(
            @PathVariable String intakeId,
            @Valid @RequestBody ConfirmIntakeResource resource
    ) {
        return toResource(confirmIntake.handle(
                new ConfirmIntakeCommand(intakeId, resource.channel())
        ));
    }

    private IntakeResource toResource(IntakeResult result) {
        return new IntakeResource(
                result.id(),
                result.treatmentId(),
                result.medicationId(),
                result.olderAdultId(),
                result.medicationName(),
                result.dose(),
                result.instructions(),
                result.scheduledAt(),
                result.status(),
                result.confirmedAt(),
                result.confirmationChannel()
        );
    }
}
