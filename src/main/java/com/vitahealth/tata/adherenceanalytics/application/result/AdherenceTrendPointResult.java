package com.vitahealth.tata.adherenceanalytics.application.result;

import java.time.LocalDate;

/** One calendar-day point in the adherence trend. */
public record AdherenceTrendPointResult(LocalDate date, int adherencePercent) {}
