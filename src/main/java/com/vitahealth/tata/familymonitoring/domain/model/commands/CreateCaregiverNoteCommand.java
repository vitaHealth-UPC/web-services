package com.vitahealth.tata.familymonitoring.domain.model.commands;

/** Requests a caregiver-owned note within the selected adult monitoring relationship. */
public record CreateCaregiverNoteCommand(String olderAdultId, String familiarId, String text) {}
