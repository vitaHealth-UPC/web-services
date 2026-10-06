package com.tata.identitysubscription.domain.model.commands;
public record VerifyEmailCommand(String email, String code) {}
