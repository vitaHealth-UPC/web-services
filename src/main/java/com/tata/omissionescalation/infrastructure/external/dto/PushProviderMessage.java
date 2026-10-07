package com.tata.omissionescalation.infrastructure.external.dto;

/** Shape expected by the push provider. It never leaves the infrastructure layer. */
public record PushProviderMessage(String recipient, String title, String body) {
}
