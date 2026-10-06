package com.tata.carelink.domain.model.commands;

public record RegisterConsentCommand(String careLinkId, boolean accepted) {}
