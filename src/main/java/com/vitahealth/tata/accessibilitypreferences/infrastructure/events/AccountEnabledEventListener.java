package com.vitahealth.tata.accessibilitypreferences.infrastructure.events;

import com.vitahealth.tata.accessibilitypreferences.interfaces.events.AccountEnabledEventConsumer;
import com.vitahealth.tata.identitysubscription.domain.model.events.AccountEnabled;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Binds Spring application events to the preference context event consumer. */
@Component
public class AccountEnabledEventListener {
  private final AccountEnabledEventConsumer consumer;

  /** Injects the context-owned event entry point. */
  public AccountEnabledEventListener(AccountEnabledEventConsumer consumer) {
    this.consumer = consumer;
  }

  /** Delivers activation in the publishing thread without starting an independent transaction. */
  @EventListener
  public void on(AccountEnabled event) {
    consumer.consume(event);
  }
}
