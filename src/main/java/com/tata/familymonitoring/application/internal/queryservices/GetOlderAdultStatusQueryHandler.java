package com.tata.familymonitoring.application.internal.queryservices;

import com.tata.familymonitoring.application.queryservices.OlderAdultStatusView;
import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.domain.model.valueobjects.OlderAdultStatus;
import com.tata.familymonitoring.domain.ports.IIntakeHistoryPort;
import com.tata.familymonitoring.domain.ports.IAdherenceSummaryPort;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOlderAdultStatusQueryHandler {

  private static final int STATUS_WINDOW_DAYS = 7;
  private static final int RECENT_INSIGHTS = 5;

  private final IFamilyMonitorRepository repository;
  private final IIntakeHistoryPort intakeHistoryPort;
  private final IAdherenceSummaryPort adherenceSummaryPort;

  public GetOlderAdultStatusQueryHandler(
      IFamilyMonitorRepository repository, IIntakeHistoryPort intakeHistoryPort, IAdherenceSummaryPort adherenceSummaryPort) {
    this.repository = repository;
    this.intakeHistoryPort = intakeHistoryPort;
    this.adherenceSummaryPort = adherenceSummaryPort;
  }

  @Transactional(readOnly = true)
  public OlderAdultStatusView handle(GetOlderAdultStatusQuery query) {
    FamilyMonitor monitor = repository.findByOlderAdultId(query.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(query.olderAdultId()));
    IntakeStatus lastIntakeStatus = intakeHistoryPort
        .getRecentIntakes(query.olderAdultId(), STATUS_WINDOW_DAYS).stream()
        .max(Comparator.comparing(IntakeSummary::scheduledAt))
        .map(IntakeSummary::status)
        .orElse(null);
    Instant nextIntakeAt = intakeHistoryPort.findNextIntakeAt(query.olderAdultId()).orElse(null);
    OlderAdultStatus status =
        new OlderAdultStatus(nextIntakeAt, lastIntakeStatus, monitor.hasOpenAlert());
    return new OlderAdultStatusView(
        status,
        monitor.openAlerts(),
        adherenceSummaryPort.getWeeklySummary(query.olderAdultId()),
        List.copyOf(monitor.getLowStockNotices()),
        monitor.recentInsights(RECENT_INSIGHTS));
  }
}
