package com.vitahealth.tata.carelink.domain.model.commands;

/** Caregiver request to stop caring for the adult of one relationship. */
public record EndCareLinkCommand(String careLinkId, String caregiverId) {}
