package com.vitahealth.tata.treatmentmanagement.interfaces.rest;

import com.vitahealth.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.RegisterMedicationRequest;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.UpdateMedicationRequest;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.MedicationResponseFromResultAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.RegisterMedicationCommandFromResourceAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResponseAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.UpdateMedicationCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes caregiver-authorized medication registration, editing and read operations. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Medications", description = "Medications of an older adult, managed by a caregiver")
public class MedicationsController {
  private final TreatmentCommandService commands;
  private final TreatmentQueryService queries;

  /** Injects public command and query services; transaction ownership stays in application. */
  public MedicationsController(TreatmentCommandService commands, TreatmentQueryService queries) {
    this.commands = commands;
    this.queries = queries;
  }

  @Operation(
      summary = "Register a medication",
      description = "Adds a medication to the older adult (US-03).")
  @ApiResponse(responseCode = "201", description = "Medication registered")
  @ApiResponse(
      responseCode = "400",
      description = "Required data is missing",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/older-adults/{olderAdultId}/medications")
  public ResponseEntity<?> register(
      @PathVariable String olderAdultId, @Valid @RequestBody RegisterMedicationRequest resource) {
    return TreatmentResponseAssembler.toResponse(
        commands.registerMedication(
            RegisterMedicationCommandFromResourceAssembler.toCommand(olderAdultId, resource)),
        HttpStatus.CREATED,
        MedicationResponseFromResultAssembler::toResource);
  }

  @Operation(
      summary = "List the medications of an older adult",
      description = "Ordered by name, including inactive ones (US-03).")
  @ApiResponse(responseCode = "200", description = "Medications returned (possibly empty)")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/older-adults/{olderAdultId}/medications")
  public ResponseEntity<?> list(
      @PathVariable String olderAdultId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        queries.handle(new ListMedicationsQuery(caregiverId, olderAdultId)),
        HttpStatus.OK,
        results ->
            results.stream().map(MedicationResponseFromResultAssembler::toResource).toList());
  }

  @Operation(
      summary = "Edit a medication",
      description =
          "Changes name and presentation of an active medication (US-04). Treatments using it "
              + "are republished so future intakes show the new name.")
  @ApiResponse(responseCode = "200", description = "Medication updated")
  @ApiResponse(
      responseCode = "400",
      description = "Required data is missing",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Medication not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "An inactive medication cannot be edited",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping("/medications/{medicationId}")
  public ResponseEntity<?> update(
      @PathVariable String medicationId, @Valid @RequestBody UpdateMedicationRequest resource) {
    return TreatmentResponseAssembler.toResponse(
        commands.updateMedication(
            UpdateMedicationCommandFromResourceAssembler.toCommand(medicationId, resource)),
        HttpStatus.OK,
        MedicationResponseFromResultAssembler::toResource);
  }

  @Operation(
      summary = "Deactivate a medication",
      description =
          "Keeps the history (US-04). An active treatment that uses the medication is paused.")
  @ApiResponse(responseCode = "200", description = "Medication deactivated")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Medication not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/medications/{medicationId}/deactivation")
  public ResponseEntity<?> deactivate(
      @PathVariable String medicationId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        commands.deactivateMedication(new DeactivateMedicationCommand(caregiverId, medicationId)),
        HttpStatus.OK,
        MedicationResponseFromResultAssembler::toResource);
  }

  @Operation(summary = "Get the detail of a medication")
  @ApiResponse(responseCode = "200", description = "Medication returned")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Medication not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/medications/{medicationId}")
  public ResponseEntity<?> detail(
      @PathVariable String medicationId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        queries.handle(new GetMedicationQuery(caregiverId, medicationId)),
        HttpStatus.OK,
        MedicationResponseFromResultAssembler::toResource);
  }
}
