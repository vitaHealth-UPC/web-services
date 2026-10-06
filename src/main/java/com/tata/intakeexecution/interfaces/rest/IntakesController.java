package com.tata.intakeexecution.interfaces.rest;

import com.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.interfaces.rest.resources.IntakeResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IntakesController {
    private final GetNextIntakeQueryHandler getNextIntake;

    public IntakesController(GetNextIntakeQueryHandler getNextIntake) {
        this.getNextIntake = getNextIntake;
    }

    @GetMapping("/older-adults/{olderAdultId}/intakes/next")
    public ResponseEntity<IntakeResource> next(@PathVariable String olderAdultId) {
        return getNextIntake.handle(olderAdultId)
                .map(this::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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
                result.status()
        );
    }
}
