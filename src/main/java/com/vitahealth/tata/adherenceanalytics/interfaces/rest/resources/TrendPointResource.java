package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import java.time.LocalDate;

/** One dated adherence trend point. */
public record TrendPointResource(LocalDate date, int adherencePercent) {}
