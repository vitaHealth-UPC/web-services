package com.tata.familymonitoring.interfaces.rest;

import com.tata.familymonitoring.application.internal.queryservices.GetContactChannelQueryHandler;
import com.tata.familymonitoring.application.internal.queryservices.GetOlderAdultStatusQueryHandler;
import com.tata.familymonitoring.application.internal.queryservices.GetRecentIntakeHistoryQueryHandler;
import com.tata.familymonitoring.domain.model.queries.GetContactChannelQuery;
import com.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.tata.familymonitoring.domain.model.queries.GetRecentIntakeHistoryQuery;
import com.tata.familymonitoring.interfaces.rest.resources.ContactChannelResource;
import com.tata.familymonitoring.interfaces.rest.resources.ErrorResource;
import com.tata.familymonitoring.interfaces.rest.resources.IntakeSummaryResource;
import com.tata.familymonitoring.interfaces.rest.resources.OlderAdultStatusResource;
import com.tata.familymonitoring.interfaces.rest.transform.ContactChannelResourceFromEntityAssembler;
import com.tata.familymonitoring.interfaces.rest.transform.IntakeSummaryResourceFromEntityAssembler;
import com.tata.familymonitoring.interfaces.rest.transform.OlderAdultStatusResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}")
@Tag(name = "Family Monitoring", description = "What a caregiver sees about an older adult")
public class FamilyMonitoringController {

  private final GetOlderAdultStatusQueryHandler statusHandler;
  private final GetRecentIntakeHistoryQueryHandler historyHandler;
  private final GetContactChannelQueryHandler contactChannelHandler;

  public FamilyMonitoringController(
      GetOlderAdultStatusQueryHandler statusHandler,
      GetRecentIntakeHistoryQueryHandler historyHandler,
      GetContactChannelQueryHandler contactChannelHandler) {
    this.statusHandler = statusHandler;
    this.historyHandler = historyHandler;
    this.contactChannelHandler = contactChannelHandler;
  }

  @Operation(
      summary = "Get the recent status of an older adult",
      description = "Next intake, last intake result and the alerts that still need attention (US-25).")
  @ApiResponse(responseCode = "200", description = "Status returned")
  @ApiResponse(responseCode = "404", description = "The older adult has no active follow-up",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @GetMapping("/status")
  public OlderAdultStatusResource getStatus(@PathVariable String olderAdultId) {
    return OlderAdultStatusResourceFromEntityAssembler.toResourceFromEntity(
        statusHandler.handle(new GetOlderAdultStatusQuery(olderAdultId)));
  }

  @Operation(
      summary = "Get the recent intake history",
      description = "Intakes of the last days, most recent first. An empty list means there are no "
          + "records in the period (US-26).")
  @ApiResponse(responseCode = "200", description = "History returned (possibly empty)")
  @ApiResponse(responseCode = "400", description = "Invalid number of days",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @ApiResponse(responseCode = "404", description = "The older adult has no active follow-up",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @GetMapping("/intakes")
  public List<IntakeSummaryResource> getRecentIntakes(
      @PathVariable String olderAdultId,
      @Parameter(description = "Days to look back, from 1 to 30")
      @RequestParam(defaultValue = "7") int days) {
    return historyHandler.handle(new GetRecentIntakeHistoryQuery(olderAdultId, days)).stream()
        .map(IntakeSummaryResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
  }

  @Operation(
      summary = "Get the contact channel of an older adult",
      description = "Channel the caregiver can use to reach the older adult after an alert (US-29).")
  @ApiResponse(responseCode = "200", description = "Contact channel returned")
  @ApiResponse(responseCode = "404", description = "No follow-up or no contact channel available",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @GetMapping("/contact-channel")
  public ContactChannelResource getContactChannel(@PathVariable String olderAdultId) {
    return ContactChannelResourceFromEntityAssembler.toResourceFromEntity(
        contactChannelHandler.handle(new GetContactChannelQuery(olderAdultId)));
  }
}
