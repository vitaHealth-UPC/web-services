package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Selects a catalog plan for the specified account without representing a payment capture. */
public record ChangeSubscriptionCommand(String accountId, String planCode) {}
