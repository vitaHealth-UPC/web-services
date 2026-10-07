package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.valueobjects.OlderAdultStatus;
import com.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import java.util.List;

/** Result of the status query: the status itself plus the alerts that still need attention. */
public record OlderAdultStatusView(OlderAdultStatus status, List<AlertSummary> openAlerts, AdherenceSnapshot weeklyAdherence) {
}
