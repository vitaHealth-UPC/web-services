package com.tata.identitysubscription.application.models;

import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;

public record AccountResult(String id, String name, String email, AccountStatus status) {}
