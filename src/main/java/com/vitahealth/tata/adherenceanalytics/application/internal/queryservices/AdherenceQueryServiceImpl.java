package com.vitahealth.tata.adherenceanalytics.application.internal.queryservices;

import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceAnalysisResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceMetricsResult;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherencePeriodQuery;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceCounts;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceInsight;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionObservation;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome.Status;
import com.vitahealth.tata.adherenceanalytics.domain.services.AdherenceInsightGenerationService;
import com.vitahealth.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates source evidence reads with domain pattern detection and insight generation. */
@Service
@Transactional(readOnly = true)
public class AdherenceQueryServiceImpl implements AdherenceQueryService {
  private final IntakeOutcomePort outcomes;
  private final int minimumOmissionDays;
  private final AdherencePatternDetectionService detector = new AdherencePatternDetectionService();

  /** Injects the source read port and validates the configured recurrence threshold. */
  public AdherenceQueryServiceImpl(
      IntakeOutcomePort outcomes,
      @Value("${adherence.pattern.minimum-omission-days:3}") int minimumOmissionDays) {
    if (minimumOmissionDays < 2 || minimumOmissionDays > 8)
      throw new IllegalArgumentException("minimum omission days must be between two and eight");
    this.outcomes = outcomes;
    this.minimumOmissionDays = minimumOmissionDays;
  }

  /** Calculates metrics from the same definitive evidence returned by history. */
  @Override
  public AdherenceMetricsResult weekly(GetAdherencePeriodQuery query) {
    var evidence = history(query);
    var period = query.period();
    return metrics(period.olderAdultId(), period.from(), period.to(), evidence);
  }

  private AdherenceMetricsResult metrics(
      String owner, Instant from, Instant to, List<IntakeOutcome> evidence) {
    var counts =
        new AdherenceCounts(
            (int) evidence.stream().filter(outcome -> outcome.status() == Status.CONFIRMED).count(),
            (int) evidence.stream().filter(outcome -> outcome.status() == Status.LATE).count(),
            (int) evidence.stream().filter(outcome -> outcome.status() == Status.OMITTED).count());
    return new AdherenceMetricsResult(
        owner.trim(),
        from,
        to,
        counts.confirmed(),
        counts.total(),
        counts.percentage(),
        counts.onTime(),
        counts.late(),
        counts.omitted());
  }

  /** Reads the bounded owner period and excludes pending schedules. */
  @Override
  public List<IntakeOutcome> history(GetAdherencePeriodQuery query) {
    var period = query.period();
    return outcomes.find(period.olderAdultId(), period.from(), period.to()).stream()
        .filter(outcome -> outcome.status() != Status.PENDING)
        .toList();
  }

  /** Delegates calendar-aware omission detection to the domain service. */
  @Override
  public List<AdherenceOmissionPattern> patterns(GetAdherencePeriodQuery query) {
    return detect(history(query), query.period().zone());
  }

  private List<AdherenceOmissionPattern> detect(List<IntakeOutcome> evidence, String zone) {
    var calendarZone = ZoneId.of(zone);
    return detector.detect(
        evidence.stream()
            .map(
                outcome ->
                    new AdherenceOmissionObservation(
                        outcome.medicationId(),
                        outcome.scheduledAt(),
                        outcome.status() == Status.OMITTED))
            .toList(),
        calendarZone,
        minimumOmissionDays);
  }

  /** Captures counts and patterns from a single evidence read. */
  @Override
  public AdherenceAnalysisResult capture(GetAdherencePeriodQuery query) {
    var evidence = history(query);
    var period = query.period();
    return new AdherenceAnalysisResult(
        metrics(period.olderAdultId(), period.from(), period.to(), evidence),
        detect(evidence, period.zone()),
        minimumOmissionDays);
  }

  /** Generates domain insights from the detected repeated omissions. */
  @Override
  public List<AdherenceInsight> recommendations(GetAdherencePeriodQuery query) {
    return new AdherenceInsightGenerationService().generate(patterns(query));
  }
}
