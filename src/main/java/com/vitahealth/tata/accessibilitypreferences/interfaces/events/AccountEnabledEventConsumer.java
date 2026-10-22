package com.vitahealth.tata.accessibilitypreferences.interfaces.events;

import com.vitahealth.tata.accessibilitypreferences.application.internal.eventhandlers.AccountEnabledEventHandler;
import com.vitahealth.tata.identitysubscription.domain.model.events.AccountEnabled;
import org.springframework.stereotype.Component;

/** Accepts identity activation events at the preference context boundary. */
@Component
public class AccountEnabledEventConsumer {
  private final AccountEnabledEventHandler eventHandler;

  /** Injects the application handler that initializes the activated owner once. */
  public AccountEnabledEventConsumer(AccountEnabledEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  /**
   * Forwards activation synchronously so initialization participates in the publisher transaction.
   */
  public void consume(AccountEnabled event) {
    eventHandler.handle(event);
  }
}
