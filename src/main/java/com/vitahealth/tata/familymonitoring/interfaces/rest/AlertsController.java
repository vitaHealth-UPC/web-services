package com.vitahealth.tata.familymonitoring.interfaces.rest;

import com.vitahealth.tata.familymonitoring.application.commandservices.CloseAlertCommandService;
import com.vitahealth.tata.familymonitoring.application.commandservices.MarkAlertAttendedCommandService;
import com.vitahealth.tata.familymonitoring.application.queryservices.CareRelationshipQueryService;
import com.vitahealth.tata.familymonitoring.application.queryservices.GetAlertDetailQueryService;
import com.vitahealth.tata.familymonitoring.application.queryservices.GetAlertTimelineQueryService;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.vitahealth.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.AlertSummaryResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.ErrorResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.RecordedAlertEventResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.UpdateAlertStatusResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.transform.AlertSummaryResourceFromResultAssembler;
import com.vitahealth.tata.familymonitoring.interfaces.rest.transform.RecordedAlertEventResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes consent-scoped alert evidence and explicit caregiver follow-up transitions. */
@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/alerts")
@Tag(name = "Alerts", description = "Follow-up of the alerts raised by omitted intakes")
@ApiResponse(
    responseCode = "401",
    description = "Authentication is required",
    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
@ApiResponse(
    responseCode = "403",
    description = "The session or current care relationship does not authorize access",
    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
@ApiResponse(
    responseCode = "500",
    description = "Monitoring state or source evidence could not be read safely",
    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
public class AlertsController {

  private final GetAlertDetailQueryService alertDetailHandler;
  private final GetAlertTimelineQueryService timeline;
  private final MarkAlertAttendedCommandService markAttendedHandler;
  private final CloseAlertCommandService closeAlertHandler;
  private final CareRelationshipQueryService access;

  /** Connects application handlers and the care-relationship authorization boundary. */
  public AlertsController(
      GetAlertDetailQueryService alertDetailHandler,
      MarkAlertAttendedCommandService markAttendedHandler,
      CloseAlertCommandService closeAlertHandler,
      CareRelationshipQueryService access,
      GetAlertTimelineQueryService timeline) {
    this.timeline = timeline;
    this.alertDetailHandler = alertDetailHandler;
    this.markAttendedHandler = markAttendedHandler;
    this.closeAlertHandler = closeAlertHandler;
    this.access = access;
  }

  /** Returns an alert belonging to the selected active follow-up. */
  @Operation(
      summary = "Get the detail of an alert",
      description = "Medication, schedule, status and reason of the alert (US-27).")
  @ApiResponse(responseCode = "200", description = "Alert returned")
  @ApiResponse(
      responseCode = "404",
      description = "Follow-up or alert not found",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @GetMapping("/{alertId}")
  public AlertSummaryResource getAlertDetail(
      @PathVariable String olderAdultId,
      @PathVariable Long alertId,
      @RequestParam String caregiverId) {
    access.check(caregiverId, olderAdultId);
    return AlertSummaryResourceFromResultAssembler.toResourceFromResult(
        alertDetailHandler.handle(new GetAlertDetailQuery(olderAdultId, caregiverId, alertId)));
  }

  /** Returns only timestamps persisted on this caregiver-owned alert. */
  @GetMapping("/{alertId}/timeline")
  @Operation(summary = "Get recorded alert timeline")
  public List<RecordedAlertEventResource> timeline(
      @PathVariable String olderAdultId,
      @PathVariable Long alertId,
      @RequestParam String caregiverId) {
    access.check(caregiverId, olderAdultId);
    return timeline.handle(new GetAlertDetailQuery(olderAdultId, caregiverId, alertId)).stream()
        .map(RecordedAlertEventResourceAssembler::from)
        .toList();
  }

  /** Dispatches attendance or closure without altering original omission evidence. */
  @Operation(
      summary = "Update the follow-up status of an alert",
      description =
          "ATTENDED registers that the caregiver acted on an open alert. CLOSED removes "
              + "it from the pending list and keeps it in the history (US-31).")
  @ApiResponse(responseCode = "200", description = "Alert updated")
  @ApiResponse(
      responseCode = "400",
      description = "Status not accepted",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Follow-up or alert not found",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The alert cannot move to that status",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @PutMapping("/{alertId}/status")
  public AlertSummaryResource updateAlertStatus(
      @PathVariable String olderAdultId,
      @PathVariable Long alertId,
      @RequestParam String caregiverId,
      @Valid @RequestBody UpdateAlertStatusResource resource) {
    access.check(caregiverId, olderAdultId);
    AlertResult alert =
        switch (resource.status()) {
          case ATTENDED ->
              markAttendedHandler.handle(
                  new MarkAlertAttendedCommand(olderAdultId, caregiverId, alertId));
          case CLOSED ->
              closeAlertHandler.handle(new CloseAlertCommand(olderAdultId, caregiverId, alertId));
          case OPEN -> throw new IllegalArgumentException("An alert cannot be moved back to OPEN");
        };
    return AlertSummaryResourceFromResultAssembler.toResourceFromResult(alert);
  }
}
