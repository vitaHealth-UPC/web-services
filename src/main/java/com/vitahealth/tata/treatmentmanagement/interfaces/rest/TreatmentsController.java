package com.vitahealth.tata.treatmentmanagement.interfaces.rest;

import com.vitahealth.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.ConfigureTreatmentRequest;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.CreateTreatmentRequest;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.ConfigureTreatmentCommandFromResourceAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.CreateTreatmentCommandFromResourceAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResponseAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResponseFromResultAssembler;
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

/**
 * Exposes regimen configuration and treatment lifecycle operations through public application
 * contracts.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Treatments",
    description = "Treatment regimen of an older adult, managed by a caregiver")
public class TreatmentsController {
  private final TreatmentCommandService commands;
  private final TreatmentQueryService queries;

  /** Injects public command and query services; transaction ownership stays in application. */
  public TreatmentsController(TreatmentCommandService commands, TreatmentQueryService queries) {
    this.commands = commands;
    this.queries = queries;
  }

  @Operation(
      summary = "Create a treatment",
      description = "Creates a treatment in DRAFT status (US-14).")
  @ApiResponse(responseCode = "201", description = "Treatment created")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/older-adults/{olderAdultId}/treatments")
  public ResponseEntity<?> create(
      @PathVariable String olderAdultId, @Valid @RequestBody CreateTreatmentRequest resource) {
    return TreatmentResponseAssembler.toResponse(
        commands.createTreatment(
            CreateTreatmentCommandFromResourceAssembler.toCommand(olderAdultId, resource)),
        HttpStatus.CREATED,
        TreatmentResponseFromResultAssembler::toResource);
  }

  @Operation(
      summary = "List the treatments of an older adult",
      description = "Oldest first (US-19).")
  @ApiResponse(responseCode = "200", description = "Treatments returned (possibly empty)")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/older-adults/{olderAdultId}/treatments")
  public ResponseEntity<?> list(
      @PathVariable String olderAdultId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        queries.handle(new ListTreatmentsQuery(caregiverId, olderAdultId)),
        HttpStatus.OK,
        results -> results.stream().map(TreatmentResponseFromResultAssembler::toResource).toList());
  }

  @Operation(
      summary = "Configure dose, frequency, schedule and reminder",
      description =
          "Defines the regimen of the treatment with an active medication (US-15, US-16, US-17). "
              + "Future intakes are regenerated from the new schedule.")
  @ApiResponse(responseCode = "200", description = "Regimen configured")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid regimen",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Treatment or medication not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The medication is inactive",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping("/treatments/{treatmentId}/regimen")
  public ResponseEntity<?> configure(
      @PathVariable String treatmentId, @Valid @RequestBody ConfigureTreatmentRequest resource) {
    return TreatmentResponseAssembler.toResponse(
        commands.configureTreatment(
            ConfigureTreatmentCommandFromResourceAssembler.toCommand(treatmentId, resource)),
        HttpStatus.OK,
        TreatmentResponseFromResultAssembler::toResource);
  }

  @Operation(
      summary = "Activate a treatment",
      description = "Only a complete treatment with an active medication can be activated (US-18).")
  @ApiResponse(responseCode = "200", description = "Treatment active")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Treatment not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The treatment is incomplete or its medication is inactive",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/treatments/{treatmentId}/activation")
  public ResponseEntity<?> activate(
      @PathVariable String treatmentId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        commands.activate(new ChangeTreatmentStatusCommand(caregiverId, treatmentId)),
        HttpStatus.OK,
        TreatmentResponseFromResultAssembler::toResource);
  }

  @Operation(
      summary = "Pause a treatment",
      description = "Pausing keeps the regimen and the history (US-18).")
  @ApiResponse(responseCode = "200", description = "Treatment paused")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Treatment not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Only an active treatment can be paused",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/treatments/{treatmentId}/pause")
  public ResponseEntity<?> pause(
      @PathVariable String treatmentId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        commands.pause(new ChangeTreatmentStatusCommand(caregiverId, treatmentId)),
        HttpStatus.OK,
        TreatmentResponseFromResultAssembler::toResource);
  }

  @Operation(summary = "Resume a paused treatment")
  @ApiResponse(responseCode = "200", description = "Treatment active again")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Treatment not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Only a paused treatment can be resumed, and its medication must be active",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/treatments/{treatmentId}/resume")
  public ResponseEntity<?> resume(
      @PathVariable String treatmentId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        commands.resume(new ChangeTreatmentStatusCommand(caregiverId, treatmentId)),
        HttpStatus.OK,
        TreatmentResponseFromResultAssembler::toResource);
  }

  @Operation(
      summary = "Get the detail of a treatment",
      description = "Treatment with its regimen (US-19).")
  @ApiResponse(responseCode = "200", description = "Treatment returned")
  @ApiResponse(
      responseCode = "403",
      description = "The caregiver has no active care link with the older adult",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Treatment not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/treatments/{treatmentId}")
  public ResponseEntity<?> detail(
      @PathVariable String treatmentId, @RequestParam String caregiverId) {
    return TreatmentResponseAssembler.toResponse(
        queries.handle(new GetTreatmentQuery(caregiverId, treatmentId)),
        HttpStatus.OK,
        TreatmentResponseFromResultAssembler::toResource);
  }
}
