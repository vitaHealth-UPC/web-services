package com.vitahealth.tata.adherenceanalytics.interfaces.rest;

import com.vitahealth.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceSnapshotQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherencePeriodQuery;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceSnapshotQuery;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.AdherencePatternResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.AdherenceRecommendationResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.AdherenceSnapshotResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.IntakeOutcomeResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.WeeklyAdherenceResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform.AdherencePatternResourceAssembler;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform.AdherenceRecommendationResourceAssembler;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform.AdherenceSnapshotResourceAssembler;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform.IntakeOutcomeResourceAssembler;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform.WeeklyAdherenceResourceAssembler;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exposes owner-scoped adherence analysis and stored consolidation evidence. */
@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/adherence")
public class AdherenceController {
  private final AdherenceQueryService queries;
  private final AdherenceConsolidationCommandService consolidations;
  private final AdherenceSnapshotQueryService snapshots;

  /** Injects separate current-analysis, consolidation and historical-evidence boundaries. */
  public AdherenceController(
      AdherenceQueryService queries,
      AdherenceConsolidationCommandService consolidations,
      AdherenceSnapshotQueryService snapshots) {
    this.queries = queries;
    this.consolidations = consolidations;
    this.snapshots = snapshots;
  }

  /** Captures or replays immutable evidence for an explicit period and calendar zone. */
  @PostMapping("/consolidations")
  public AdherenceSnapshotResource consolidate(
      @PathVariable String olderAdultId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "UTC") String zone) {
    return AdherenceSnapshotResourceAssembler.from(
        consolidations.handle(new ConsolidateWeeklyPeriodCommand(olderAdultId, from, to, zone)));
  }

  /** Returns stored evidence only when the requested owner matches. */
  @GetMapping("/consolidations/{snapshotId}")
  public AdherenceSnapshotResource snapshot(
      @PathVariable String olderAdultId, @PathVariable String snapshotId) {
    return snapshots
        .handle(new GetAdherenceSnapshotQuery(olderAdultId, snapshotId))
        .map(AdherenceSnapshotResourceAssembler::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Returns definitive intake metrics for the explicit period. */
  @GetMapping("/weekly")
  public WeeklyAdherenceResource weekly(
      @PathVariable String olderAdultId, @RequestParam Instant from, @RequestParam Instant to) {
    return WeeklyAdherenceResourceAssembler.from(
        queries.weekly(new GetAdherencePeriodQuery(olderAdultId, from, to)));
  }

  /** Returns repeated omission patterns as dedicated resources. */
  @GetMapping("/patterns")
  public List<AdherencePatternResource> patterns(
      @PathVariable String olderAdultId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "UTC") String zone) {
    return queries.patterns(new GetAdherencePeriodQuery(olderAdultId, from, to, zone)).stream()
        .map(AdherencePatternResourceAssembler::from)
        .toList();
  }

  /** Returns definitive outcome history without exposing domain types. */
  @GetMapping("/history")
  public List<IntakeOutcomeResource> history(
      @PathVariable String olderAdultId, @RequestParam Instant from, @RequestParam Instant to) {
    return queries.history(new GetAdherencePeriodQuery(olderAdultId, from, to)).stream()
        .map(IntakeOutcomeResourceAssembler::from)
        .toList();
  }

  /** Returns evidence-supported follow-up recommendations. */
  @GetMapping("/recommendations")
  public List<AdherenceRecommendationResource> recommendations(
      @PathVariable String olderAdultId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "UTC") String zone) {
    return queries
        .recommendations(new GetAdherencePeriodQuery(olderAdultId, from, to, zone))
        .stream()
        .map(AdherenceRecommendationResourceAssembler::from)
        .toList();
  }
}
