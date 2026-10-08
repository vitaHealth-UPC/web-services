package com.tata.omissionescalation.infrastructure.external;

import com.tata.omissionescalation.infrastructure.external.dto.PushProviderMessage;

/**
 * Minimal provider-facing contract used by the push ACL.
 *
 * Implementations may wrap FCM, APNs through a gateway, or another provider without exposing
 * provider DTOs outside Infrastructure.
 */
@FunctionalInterface
public interface PushProviderClient {

  void send(PushProviderMessage message);
}
