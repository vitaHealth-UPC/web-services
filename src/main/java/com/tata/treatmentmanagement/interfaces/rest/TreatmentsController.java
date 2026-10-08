package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.tata.treatmentmanagement.domain.model.commands.*;
import com.tata.treatmentmanagement.interfaces.rest.resources.TreatmentResources.*;
import com.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Treatments", description = "Treatment regimen of an older adult, managed by a caregiver")
public class TreatmentsController {
    private final TreatmentCommandService commands;
    private final TreatmentQueryService queries;

    public TreatmentsController(TreatmentCommandService commands, TreatmentQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @Operation(summary = "Create a treatment", description = "Creates a treatment in DRAFT status (US-14).")
    @ApiResponse(responseCode = "201", description = "Treatment created")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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

    @Operation(summary = "List the treatments of an older adult", description = "Oldest first (US-19).")
    @ApiResponse(responseCode = "200", description = "Treatments returned (possibly empty)")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/older-adults/{olderAdultId}/treatments")
    public List<TreatmentResponse> list(
            @PathVariable String olderAdultId,
            @RequestParam String caregiverId
    ) {
        return queries.listTreatments(caregiverId, olderAdultId).stream()
                .map(TreatmentResourceAssembler::toResource)
                .toList();
    }

    @Operation(
            summary = "Configure dose, frequency, schedule and reminder",
            description = "Defines the regimen of the treatment with an active medication (US-15, US-16, US-17). "
                    + "Future intakes are regenerated from the new schedule.")
    @ApiResponse(responseCode = "200", description = "Regimen configured")
    @ApiResponse(responseCode = "400", description = "Invalid regimen",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Treatment or medication not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The medication is inactive",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/treatments/{treatmentId}/regimen")
    public TreatmentResponse configure(
            @PathVariable String treatmentId,
            @Valid @RequestBody ConfigureTreatmentRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(commands.configureTreatment(
                new ConfigureTreatmentCommand(
                        resource.caregiverId(), treatmentId, resource.medicationId(), resource.dose(), resource.frequency(),
                        resource.scheduledTimes(), resource.instructions(), resource.reminderLeadMinutes()
                )
        ));
    }

    @Operation(summary = "Activate a treatment", description = "Only a complete treatment with an active medication can be activated (US-18).")
    @ApiResponse(responseCode = "200", description = "Treatment active")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Treatment not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The treatment is incomplete or its medication is inactive",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/treatments/{treatmentId}/activation")
    public TreatmentResponse activate(
            @PathVariable String treatmentId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(
                commands.activate(new ChangeTreatmentStatusCommand(caregiverId, treatmentId))
        );
    }

    @Operation(summary = "Pause a treatment", description = "Pausing keeps the regimen and the history (US-18).")
    @ApiResponse(responseCode = "200", description = "Treatment paused")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Treatment not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Only an active treatment can be paused",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/treatments/{treatmentId}/pause")
    public TreatmentResponse pause(
            @PathVariable String treatmentId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(
                commands.pause(new ChangeTreatmentStatusCommand(caregiverId, treatmentId))
        );
    }

    @Operation(summary = "Resume a paused treatment")
    @ApiResponse(responseCode = "200", description = "Treatment active again")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Treatment not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Only a paused treatment can be resumed, and its medication must be active",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/treatments/{treatmentId}/resume")
    public TreatmentResponse resume(
            @PathVariable String treatmentId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(
                commands.resume(new ChangeTreatmentStatusCommand(caregiverId, treatmentId))
        );
    }

    @Operation(summary = "Get the detail of a treatment", description = "Treatment with its regimen (US-19).")
    @ApiResponse(responseCode = "200", description = "Treatment returned")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Treatment not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/treatments/{treatmentId}")
    public TreatmentResponse detail(
            @PathVariable String treatmentId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(queries.getTreatment(caregiverId, treatmentId));
    }
}
