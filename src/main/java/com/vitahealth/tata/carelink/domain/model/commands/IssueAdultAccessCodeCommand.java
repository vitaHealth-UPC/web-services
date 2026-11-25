package com.vitahealth.tata.carelink.domain.model.commands;

/** Caregiver request for a new sign-in code for the adult of a confirmed relationship. */
public record IssueAdultAccessCodeCommand(String careLinkId, String caregiverId) {}
