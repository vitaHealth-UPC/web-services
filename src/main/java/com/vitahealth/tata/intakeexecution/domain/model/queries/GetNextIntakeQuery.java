package com.vitahealth.tata.intakeexecution.domain.model.queries;

/** Selection of the next pending intake for one adult. */
public record GetNextIntakeQuery(String olderAdultId) {}
