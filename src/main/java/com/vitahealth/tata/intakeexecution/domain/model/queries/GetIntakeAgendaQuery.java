package com.vitahealth.tata.intakeexecution.domain.model.queries;

import java.time.Instant;

/** Agenda selection over an explicit half-open instant interval. */
public record GetIntakeAgendaQuery(String olderAdultId, Instant from, Instant to) {}
