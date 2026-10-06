package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.tata.treatmentmanagement.domain.model.commands.*;
import com.tata.treatmentmanagement.interfaces.rest.resources.TreatmentResources.*;
import com.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TreatmentsController {
    private final TreatmentCommandService commands;
    private final TreatmentQueryService queries;

    public TreatmentsController(TreatmentCommandService commands, TreatmentQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @PostMapping("/older-adults/{olderAdultId}/treatments")
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentResponse create(
            @PathVariable String olderAdultId,
            @Valid @RequestBody CreateTreatmentRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(
                commands.createTreatment(new CreateTreatmentCommand(resource.caregiverId(), olderAdultId, resource.name()))
        );
    }

    @PutMapping("/treatments/{treatmentId}/regimen")
    public TreatmentResponse configure(
            @PathVariable String treatmentId,
            @Valid @RequestBody ConfigureTreatmentRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(commands.configureTreatment(
                new ConfigureTreatmentCommand(
                        treatmentId, resource.medicationId(), resource.dose(), resource.frequency(),
                        resource.scheduledTime(), resource.instructions(), resource.reminderLeadMinutes()
                )
        ));
    }

    @PostMapping("/treatments/{treatmentId}/activation")
    public TreatmentResponse activate(@PathVariable String treatmentId) {
        return TreatmentResourceAssembler.toResource(
                commands.activate(new ChangeTreatmentStatusCommand(treatmentId))
        );
    }

    @PostMapping("/treatments/{treatmentId}/pause")
    public TreatmentResponse pause(@PathVariable String treatmentId) {
        return TreatmentResourceAssembler.toResource(
                commands.pause(new ChangeTreatmentStatusCommand(treatmentId))
        );
    }

    @PostMapping("/treatments/{treatmentId}/resume")
    public TreatmentResponse resume(@PathVariable String treatmentId) {
        return TreatmentResourceAssembler.toResource(
                commands.resume(new ChangeTreatmentStatusCommand(treatmentId))
        );
    }

    @GetMapping("/treatments/{treatmentId}")
    public TreatmentResponse detail(
            @PathVariable String treatmentId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(queries.getTreatment(caregiverId, treatmentId));
    }
}
