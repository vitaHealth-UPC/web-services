package com.tata.identitysubscription.domain.model.commands;

public record ChangeSubscriptionCommand(String accountId, String planCode) {
}
