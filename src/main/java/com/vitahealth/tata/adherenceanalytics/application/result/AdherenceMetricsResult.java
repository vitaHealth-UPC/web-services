package com.vitahealth.tata.adherenceanalytics.application.result;

import java.time.Instant;

/** Captured definitive intake counts and adherence percentage for one owner and period. */
public record AdherenceMetricsResult(
    String olderAdultId,
    Instant from,
    Instant to,
    int confirmedIntakes,
    int totalIntakes,
    double percentage,
    int onTimeIntakes,
    int lateIntakes,
    int omittedIntakes) {}
