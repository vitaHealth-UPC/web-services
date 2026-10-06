package com.tata.familymonitoring.application.internal.queryservices;

import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.queries.GetRecentIntakeHistoryQuery;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.domain.ports.IIntakeHistoryPort;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetRecentIntakeHistoryQueryHandler {

  public static final int MAX_DAYS = 30;

  private final IFamilyMonitorRepository repository;
  private final IIntakeHistoryPort intakeHistoryPort;

  public GetRecentIntakeHistoryQueryHandler(
      IFamilyMonitorRepository repository, IIntakeHistoryPort intakeHistoryPort) {
    this.repository = repository;
    this.intakeHistoryPort = intakeHistoryPort;
  }

  /** Most recent intakes first. An empty list means there are no records in the period. */
  public List<IntakeSummary> handle(GetRecentIntakeHistoryQuery query) {
    if (query.days() < 1 || query.days() > MAX_DAYS) {
      throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
    }
    repository.findByOlderAdultId(query.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(query.olderAdultId()));
    return intakeHistoryPort.getRecentIntakes(query.olderAdultId(), query.days()).stream()
        .sorted(Comparator.comparing(IntakeSummary::scheduledAt).reversed())
        .toList();
  }
}
