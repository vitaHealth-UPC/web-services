package com.tata.omissionescalation.infrastructure.external;

import com.tata.omissionescalation.infrastructure.external.dto.PushProviderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Safe development provider. A production adapter can replace this bean without changing
 * application or domain code.
 */
@Component
public class LoggingPushProviderClient implements PushProviderClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPushProviderClient.class);

  @Override
  public void send(PushProviderMessage message) {
    LOGGER.info("Push to {}: {} - {}", message.recipient(), message.title(), message.body());
  }
}
