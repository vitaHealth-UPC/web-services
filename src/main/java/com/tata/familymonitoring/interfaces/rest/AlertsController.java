package com.tata.familymonitoring.interfaces.rest;

import com.tata.familymonitoring.application.internal.commandservices.CloseAlertCommandHandler;
import com.tata.familymonitoring.application.internal.commandservices.MarkAlertAttendedCommandHandler;
import com.tata.familymonitoring.application.internal.queryservices.GetAlertDetailQueryHandler;
import com.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;
import com.tata.familymonitoring.interfaces.rest.resources.AlertSummaryResource;
import com.tata.familymonitoring.interfaces.rest.resources.ErrorResource;
import com.tata.familymonitoring.interfaces.rest.resources.UpdateAlertStatusResource;
import com.tata.familymonitoring.interfaces.rest.transform.AlertSummaryResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/alerts")
@Tag(name = "Alerts", description = "Follow-up of the alerts raised by omitted intakes")
public class AlertsController {

  private final GetAlertDetailQueryHandler alertDetailHandler;
  private final MarkAlertAttendedCommandHandler markAttendedHandler;
  private final CloseAlertCommandHandler closeAlertHandler;

  public AlertsController(
      GetAlertDetailQueryHandler alertDetailHandler,
      MarkAlertAttendedCommandHandler markAttendedHandler,
      CloseAlertCommandHandler closeAlertHandler) {
    this.alertDetailHandler = alertDetailHandler;
    this.markAttendedHandler = markAttendedHandler;
    this.closeAlertHandler = closeAlertHandler;
  }

  @Operation(
      summary = "Get the detail of an alert",
      description = "Medication, schedule, status and reason of the alert (US-27).")
  @ApiResponse(responseCode = "200", description = "Alert returned")
  @ApiResponse(responseCode = "404", description = "Follow-up or alert not found",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @GetMapping("/{alertId}")
  public AlertSummaryResource getAlertDetail(
      @PathVariable Long olderAdultId, @PathVariable Long alertId) {
    return AlertSummaryResourceFromEntityAssembler.toResourceFromEntity(
        alertDetailHandler.handle(new GetAlertDetailQuery(olderAdultId, alertId)));
  }

  @Operation(
      summary = "Update the follow-up status of an alert",
      description = "ATTENDED registers that the caregiver acted on an open alert. CLOSED removes "
          + "it from the pending list and keeps it in the history (US-31).")
  @ApiResponse(responseCode = "200", description = "Alert updated")
  @ApiResponse(responseCode = "400", description = "Status not accepted",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @ApiResponse(responseCode = "404", description = "Follow-up or alert not found",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @ApiResponse(responseCode = "409", description = "The alert cannot move to that status",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @PutMapping("/{alertId}/status")
  public AlertSummaryResource updateAlertStatus(
      @PathVariable Long olderAdultId,
      @PathVariable Long alertId,
      @Valid @RequestBody UpdateAlertStatusResource resource) {
    AlertSummary alert = switch (resource.status()) {
      case ATTENDED -> markAttendedHandler.handle(new MarkAlertAttendedCommand(olderAdultId, alertId));
      case CLOSED -> closeAlertHandler.handle(new CloseAlertCommand(olderAdultId, alertId));
      case OPEN -> throw new IllegalArgumentException("An alert cannot be moved back to OPEN");
    };
    return AlertSummaryResourceFromEntityAssembler.toResourceFromEntity(alert);
  }
}
