package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.GetOlderAdultStatusQueryService;
import com.vitahealth.tata.familymonitoring.application.result.AdherenceInsightResult;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.application.result.LowStockNoticeResult;
import com.vitahealth.tata.familymonitoring.application.result.OlderAdultStatusResult;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.OlderAdultStatus;
import com.vitahealth.tata.familymonitoring.domain.ports.AdherenceSummaryPort;
import com.vitahealth.tata.familymonitoring.domain.ports.IntakeHistoryPort;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads adult evidence after selecting the requesting caregiver's follow-up. */
@Service
public class GetOlderAdultStatusQueryHandler implements GetOlderAdultStatusQueryService {

  private static final int STATUS_WINDOW_DAYS = 7;
  private static final int RECENT_INSIGHTS = 5;

  private final FamilyMonitorSelectionService selection;
  private final IntakeHistoryPort intakeHistoryPort;
  private final AdherenceSummaryPort adherenceSummaryPort;

  /** Injects follow-up storage and the public source-context query ports. */
  public GetOlderAdultStatusQueryHandler(
      FamilyMonitorSelectionService selection,
      IntakeHistoryPort intakeHistoryPort,
      AdherenceSummaryPort adherenceSummaryPort) {
    this.selection = selection;
    this.intakeHistoryPort = intakeHistoryPort;
    this.adherenceSummaryPort = adherenceSummaryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public OlderAdultStatusResult handle(GetOlderAdultStatusQuery query) {
    FamilyMonitor monitor = selection.requireCurrent(query.olderAdultId(), query.caregiverId());
    IntakeStatus lastIntakeStatus =
        intakeHistoryPort.getRecentIntakes(query.olderAdultId(), STATUS_WINDOW_DAYS).stream()
            .max(Comparator.comparing(IntakeSummary::scheduledAt))
            .map(IntakeSummary::status)
            .orElse(null);
    Instant nextIntakeAt = intakeHistoryPort.findNextIntakeAt(query.olderAdultId()).orElse(null);
    OlderAdultStatus status =
        new OlderAdultStatus(nextIntakeAt, lastIntakeStatus, monitor.hasPendingAlerts());
    return new OlderAdultStatusResult(
        status,
        monitor.pendingAlerts().stream().map(AlertResult::from).toList(),
        adherenceSummaryPort.getWeeklySummary(query.olderAdultId()),
        monitor.getLowStockNotices().stream().map(LowStockNoticeResult::from).toList(),
        monitor.recentInsights(RECENT_INSIGHTS).stream()
            .map(AdherenceInsightResult::from)
            .toList());
  }
}
