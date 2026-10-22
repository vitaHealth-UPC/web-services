package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.GetRecentIntakeHistoryQueryService;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetRecentIntakeHistoryQuery;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.vitahealth.tata.familymonitoring.domain.ports.IntakeHistoryPort;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Reads adult evidence after selecting the requesting caregiver's follow-up. */
@Service
public class GetRecentIntakeHistoryQueryHandler implements GetRecentIntakeHistoryQueryService {

  public static final int MAX_DAYS = 30;

  private final FamilyMonitorSelectionService selection;
  private final IntakeHistoryPort intakeHistoryPort;

  /** Injects follow-up storage and the public source-context query ports. */
  public GetRecentIntakeHistoryQueryHandler(
      FamilyMonitorSelectionService selection, IntakeHistoryPort intakeHistoryPort) {
    this.selection = selection;
    this.intakeHistoryPort = intakeHistoryPort;
  }

  /** Most recent intakes first. An empty list means there are no records in the period. */
  public List<IntakeSummary> handle(GetRecentIntakeHistoryQuery query) {
    if (query.days() < 1 || query.days() > MAX_DAYS) {
      throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
    }
    selection.requireCurrent(query.olderAdultId(), query.caregiverId());
    return intakeHistoryPort.getRecentIntakes(query.olderAdultId(), query.days()).stream()
        .sorted(Comparator.comparing(IntakeSummary::scheduledAt).reversed())
        .toList();
  }
}
