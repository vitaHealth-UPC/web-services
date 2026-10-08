package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.tata.treatmentmanagement.interfaces.rest.resources.MedicationResources.*;
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
@Tag(name = "Medications", description = "Medications of an older adult, managed by a caregiver")
public class MedicationsController {
    private final TreatmentCommandService commands;
    private final TreatmentQueryService queries;

    public MedicationsController(TreatmentCommandService commands, TreatmentQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @Operation(summary = "Register a medication", description = "Adds a medication to the older adult (US-03).")
    @ApiResponse(responseCode = "201", description = "Medication registered")
    @ApiResponse(responseCode = "400", description = "Required data is missing",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/older-adults/{olderAdultId}/medications")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationResponse register(
            @PathVariable String olderAdultId,
            @Valid @RequestBody RegisterMedicationRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(commands.registerMedication(
                new RegisterMedicationCommand(resource.caregiverId(), olderAdultId, resource.name(), resource.presentation())
        ));
    }

    @Operation(summary = "List the medications of an older adult", description = "Ordered by name, including inactive ones (US-03).")
    @ApiResponse(responseCode = "200", description = "Medications returned (possibly empty)")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/older-adults/{olderAdultId}/medications")
    public List<MedicationResponse> list(
            @PathVariable String olderAdultId,
            @RequestParam String caregiverId
    ) {
        return queries.listMedications(caregiverId, olderAdultId).stream()
                .map(TreatmentResourceAssembler::toResource)
                .toList();
    }

    @Operation(
            summary = "Edit a medication",
            description = "Changes name and presentation of an active medication (US-04). Treatments using it "
                    + "are republished so future intakes show the new name.")
    @ApiResponse(responseCode = "200", description = "Medication updated")
    @ApiResponse(responseCode = "400", description = "Required data is missing",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Medication not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "An inactive medication cannot be edited",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/medications/{medicationId}")
    public MedicationResponse update(
            @PathVariable String medicationId,
            @Valid @RequestBody UpdateMedicationRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(commands.updateMedication(
                new UpdateMedicationCommand(resource.caregiverId(), medicationId, resource.name(), resource.presentation())
        ));
    }

    @Operation(
            summary = "Deactivate a medication",
            description = "Keeps the history (US-04). An active treatment that uses the medication is paused.")
    @ApiResponse(responseCode = "200", description = "Medication deactivated")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Medication not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/medications/{medicationId}/deactivation")
    public MedicationResponse deactivate(
            @PathVariable String medicationId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(
                commands.deactivateMedication(new DeactivateMedicationCommand(caregiverId, medicationId))
        );
    }

    @Operation(summary = "Get the detail of a medication")
    @ApiResponse(responseCode = "200", description = "Medication returned")
    @ApiResponse(responseCode = "403", description = "The caregiver has no active care link with the older adult",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Medication not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/medications/{medicationId}")
    public MedicationResponse detail(
            @PathVariable String medicationId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(queries.getMedication(caregiverId, medicationId));
    }
}
