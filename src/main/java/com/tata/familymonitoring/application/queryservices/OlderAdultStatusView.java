package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.entities.LowStockNotice;
import com.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.tata.familymonitoring.domain.model.valueobjects.OlderAdultStatus;
import java.util.List;

/**
 * Result of the status query: the status itself, the alerts that still need attention, the weekly
 * adherence, the medications running out and the latest adherence patterns.
 */
public record OlderAdultStatusView(
    OlderAdultStatus status,
    List<AlertSummary> openAlerts,
    AdherenceSnapshot weeklyAdherence,
    List<LowStockNotice> lowStockNotices,
    List<AdherenceInsight> insights) {
}
