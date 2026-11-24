package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertSeverity;
import com.vitahealth.tata.familymonitoring.domain.ports.AlertSeverityPort;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Adds the same priority evidence to status, detail and transition receipts after authorization.
 */
@Service
public class AlertResultProjection {
  private final AlertSeverityPort severity;

  public AlertResultProjection(AlertSeverityPort severity) {
    this.severity = severity;
  }

  public AlertResult project(String olderAdultId, AlertSummary alert) {
    return project(olderAdultId, List.of(alert)).getFirst();
  }

  /** Queries a batch once and never replaces unavailable evidence with a reassuring default. */
  public List<AlertResult> project(String olderAdultId, List<AlertSummary> alerts) {
    if (alerts.isEmpty()) return List.of();
    var ids = alerts.stream().map(AlertSummary::getIntakeId).collect(Collectors.toSet());
    var levels = severity.forIntakes(olderAdultId, ids);
    return alerts.stream()
        .map(
            alert ->
                AlertResult.from(
                    alert, levels.getOrDefault(alert.getIntakeId(), AlertSeverity.UNKNOWN)))
        .toList();
  }
}
